"""
optimizer.py
MILP-based cost optimisation engine for RwandAir aircraft delay recovery.

Uses PuLP with the CBC solver to enumerate all feasible recovery options, compute
their costs, then formally minimise total cost over a binary-selection linear
programme so the mathematically cheapest option is guaranteed to be ranked first.
"""

from __future__ import annotations

import logging
from typing import List, Tuple

import pulp

from models import DelayCategory, OptimisationRequest, RecoveryOptionResult
from option_scoring import crew_duty_status, passenger_impact_score

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Distance look-up table (origin → approximate flight hours from Kigali hub)
# Used to estimate positioning fuel when a spare aircraft must be ferried.
# ---------------------------------------------------------------------------
AIRPORT_DISTANCE_HOURS: dict[str, float] = {
    "KGL": 0.0,
    "NBO": 1.5,
    "EBB": 0.8,
    "DAR": 1.8,
    "JRO": 1.9,
    "ADD": 2.0,
    "LOS": 3.5,
    "JNB": 4.0,
    "CPT": 4.8,
    "CDG": 8.5,
    "BRU": 8.5,
    "LHR": 8.7,
    "DXB": 5.0,
    "DOH": 5.2,
}

AVERAGE_FLIGHT_HOURS = 2.5  # fallback positioning estimate


def _positioning_hours(aircraft_location: str, origin: str) -> float:
    """Estimate ferry hours between a spare aircraft's base and the origin airport."""
    loc_h = AIRPORT_DISTANCE_HOURS.get(aircraft_location.upper(), AVERAGE_FLIGHT_HOURS)
    orig_h = AIRPORT_DISTANCE_HOURS.get(origin.upper(), AVERAGE_FLIGHT_HOURS)
    return max(abs(loc_h - orig_h), 0.5)  # at least 30-min positioning cost


# ---------------------------------------------------------------------------
# Individual option builders
# ---------------------------------------------------------------------------

def _build_delay_absorption(req: OptimisationRequest) -> RecoveryOptionResult:
    """
    The aircraft stays on the ground and absorbs the delay.
    Costs:
      - Crew overtime for every crew member whose remaining duty hours
        are exceeded by the delay.
      - ATC slot penalty for the delayed departure.
      - Passenger compensation for connection passengers who miss onward flights.
    """
    delay_hours = req.delay_minutes / 60.0

    crew_overtime_cost = 0.0
    infeasible_crew: List[str] = []

    for crew in req.available_crew:
        duty_remaining = float(crew.get("duty_hours_remaining", 4.0))
        overtime_rate = float(crew.get("overtime_rate", 80.0))
        overtime_hours = max(0.0, delay_hours - duty_remaining)
        if overtime_hours > 0:
            crew_overtime_cost += overtime_hours * overtime_rate
        # Flag crew members with zero duty remaining (truly infeasible)
        if duty_remaining <= 0:
            infeasible_crew.append(str(crew.get("id", "?")))

    slot_penalty = delay_hours * req.slot_penalty_per_hour
    pax_compensation = req.connection_passengers * req.passenger_compensation_rate
    fuel_cost = 0.0
    mro_cost = 0.0

    total_cost = crew_overtime_cost + slot_penalty + pax_compensation

    feasible = len(infeasible_crew) == 0
    notes = (
        "All crew within legal duty limits."
        if feasible
        else f"Crew IDs {infeasible_crew} have exhausted duty hours; regulatory violation."
    )

    crew_duty_compliance, regulatory_feasible = crew_duty_status(req.available_crew, delay_hours, exempt=False)

    return RecoveryOptionResult(
        option_type="DELAY_ABSORPTION",
        description=(
            f"Absorb {req.delay_minutes}-minute delay on ground. "
            f"Crew overtime costs apply for those exceeding duty limits."
        ),
        total_cost=round(total_cost, 2),
        crew_overtime_cost=round(crew_overtime_cost, 2),
        fuel_cost=round(fuel_cost, 2),
        passenger_compensation_cost=round(pax_compensation, 2),
        slot_penalty_cost=round(slot_penalty, 2),
        mro_cost=round(mro_cost, 2),
        estimated_delay_reduction=0,
        feasible=feasible,
        feasibility_notes=notes,
        generated_by="MILP",
        passenger_impact_score=passenger_impact_score(
            "DELAY_ABSORPTION", req.delay_minutes, 0, req.passenger_count, req.connection_passengers
        ),
        crew_duty_compliance=crew_duty_compliance,
        regulatory_feasible=regulatory_feasible,
    )


def _build_flight_cancellation(req: OptimisationRequest) -> RecoveryOptionResult:
    """
    Cancel the flight entirely.
    Costs:
      - Full passenger compensation + hotel + rebooking (1.5× rate per PAX).
      - Revenue loss estimated at USD 350 average ticket.
    Crew and fuel costs are zero (crew released, no fuel burned).
    """
    pax_compensation = req.passenger_count * req.passenger_compensation_rate * 1.5
    revenue_loss = req.passenger_count * 350.0
    total_cost = pax_compensation + revenue_loss

    return RecoveryOptionResult(
        option_type="FLIGHT_CANCELLATION",
        description=(
            f"Cancel flight {req.flight_number} ({req.origin}→{req.destination}). "
            f"All {req.passenger_count} passengers receive full refund + hotel."
        ),
        total_cost=round(total_cost, 2),
        crew_overtime_cost=0.0,
        fuel_cost=0.0,
        passenger_compensation_cost=round(pax_compensation, 2),
        slot_penalty_cost=0.0,
        mro_cost=round(revenue_loss, 2),  # revenue loss recorded in mro_cost field
        estimated_delay_reduction=req.delay_minutes,  # flight disappears → "no delay"
        feasible=True,
        feasibility_notes="Always feasible but highest cost; last-resort option.",
        generated_by="MILP",
        passenger_impact_score=passenger_impact_score(
            "FLIGHT_CANCELLATION", req.delay_minutes, req.delay_minutes, req.passenger_count, req.connection_passengers
        ),
        crew_duty_compliance="COMPLIANT",
        regulatory_feasible=True,
    )


def _build_aircraft_swap(
    req: OptimisationRequest, spare: dict
) -> RecoveryOptionResult:
    """
    Swap in a spare aircraft.
    Costs:
      - Positioning fuel (ferry flight to origin).
      - MRO check cost if the aircraft needs a quick inspection.
      - Passenger compensation for any additional delay caused by swap.
    Feasibility:
      - Spare must be SERVICEABLE.
      - Spare must have ≥ passenger_count seats.
    """
    spare_status = str(spare.get("status", "SERVICEABLE")).upper()
    spare_seats = int(spare.get("seats", 0))
    spare_location = str(spare.get("location", "KGL"))
    spare_id = spare.get("id", "?")
    last_maint = float(spare.get("last_maintenance_hours", 0.0))

    feasible = spare_status == "SERVICEABLE" and spare_seats >= req.passenger_count
    notes_parts: List[str] = []

    if spare_status != "SERVICEABLE":
        notes_parts.append(f"Aircraft {spare_id} is {spare_status}, not airworthy.")
    if spare_seats < req.passenger_count:
        notes_parts.append(
            f"Aircraft {spare_id} has only {spare_seats} seats; need {req.passenger_count}."
        )

    ferry_hours = _positioning_hours(spare_location, req.origin)
    fuel_cost = ferry_hours * req.fuel_cost_per_hour

    mro_cost = 500.0 if last_maint < 10.0 else 0.0  # quick check if recently maintained

    # Swap introduces an extra delay equal to ferry time
    swap_delay_minutes = int(ferry_hours * 60)
    # Connection passengers still affected by the extra swap delay
    swap_delay_hours = swap_delay_minutes / 60.0
    pax_compensation = (
        req.connection_passengers
        * req.passenger_compensation_rate
        * (swap_delay_hours / (req.delay_minutes / 60.0 + 0.001))
    )

    total_cost = fuel_cost + mro_cost + pax_compensation
    delay_reduction = max(0, req.delay_minutes - swap_delay_minutes)

    if feasible:
        notes_parts.append(
            f"Spare aircraft {spare_id} ({spare.get('type','?')}) at {spare_location}. "
            f"Ferry time ≈ {ferry_hours:.1f} h. Net delay reduction: {delay_reduction} min."
        )

    return RecoveryOptionResult(
        option_type="AIRCRAFT_SWAP",
        description=(
            f"Swap in spare aircraft {spare_id} ({spare.get('type','?')}) "
            f"from {spare_location} for flight {req.flight_number}."
        ),
        total_cost=round(total_cost, 2),
        crew_overtime_cost=0.0,
        fuel_cost=round(fuel_cost, 2),
        passenger_compensation_cost=round(pax_compensation, 2),
        slot_penalty_cost=0.0,
        mro_cost=round(mro_cost, 2),
        estimated_delay_reduction=delay_reduction,
        feasible=feasible,
        feasibility_notes=" ".join(notes_parts) or "Feasible.",
        generated_by="MILP",
        passenger_impact_score=passenger_impact_score(
            "AIRCRAFT_SWAP", req.delay_minutes, delay_reduction, req.passenger_count, req.connection_passengers
        ),
        crew_duty_compliance="COMPLIANT",
        regulatory_feasible=True,
    )


def _build_crew_substitution(req: OptimisationRequest) -> RecoveryOptionResult:
    """
    Substitute crew members approaching duty limits.
    Costs:
      - Transport cost per substituted crew member (USD 200).
      - 30-minute extra delay for briefing.
      - Remaining slot penalty for the briefing delay.
    Feasibility:
      - At least one qualified replacement must be available.
    """
    aircraft_type = req.aircraft_type or "B737"
    delay_hours = req.delay_minutes / 60.0

    # Identify crew near duty limit (< 1 h remaining)
    near_limit = [
        c for c in req.available_crew if float(c.get("duty_hours_remaining", 10.0)) < 1.0
    ]
    # Identify qualified replacements (duty > 3 h remaining)
    replacements = [
        c
        for c in req.available_crew
        if float(c.get("duty_hours_remaining", 0.0)) >= 3.0
        and aircraft_type.upper() in str(c.get("qualification", "")).upper()
    ]

    feasible = len(replacements) > 0
    substituted_count = min(len(near_limit), len(replacements))

    transport_cost = substituted_count * 200.0
    briefing_delay_hours = 0.5  # 30-minute briefing
    slot_penalty = briefing_delay_hours * req.slot_penalty_per_hour
    pax_compensation = (
        req.connection_passengers
        * req.passenger_compensation_rate
        * (briefing_delay_hours / (delay_hours + 0.001))
    )

    # Overtime avoided from original crew
    overtime_avoided = sum(
        max(0.0, delay_hours - float(c.get("duty_hours_remaining", delay_hours)))
        * float(c.get("overtime_rate", 80.0))
        for c in near_limit
    )

    total_cost = transport_cost + slot_penalty + pax_compensation - overtime_avoided * 0.5
    total_cost = max(total_cost, 0.0)

    delay_reduction = max(0, req.delay_minutes - 30)  # saves delay minus briefing time

    return RecoveryOptionResult(
        option_type="CREW_SUBSTITUTION",
        description=(
            f"Substitute {substituted_count} crew member(s) approaching duty limits. "
            f"30-minute extra briefing required."
        ),
        total_cost=round(total_cost, 2),
        crew_overtime_cost=round(transport_cost, 2),
        fuel_cost=0.0,
        passenger_compensation_cost=round(pax_compensation, 2),
        slot_penalty_cost=round(slot_penalty, 2),
        mro_cost=0.0,
        estimated_delay_reduction=delay_reduction,
        feasible=feasible,
        feasibility_notes=(
            f"{len(replacements)} qualified replacement(s) available."
            if feasible
            else "No qualified replacement crew available with sufficient duty hours."
        ),
        generated_by="MILP",
        passenger_impact_score=passenger_impact_score(
            "CREW_SUBSTITUTION", req.delay_minutes, delay_reduction, req.passenger_count, req.connection_passengers
        ),
        crew_duty_compliance="COMPLIANT" if feasible else "VIOLATION",
        regulatory_feasible=feasible,
    )


def _build_passenger_rerouting(req: OptimisationRequest) -> RecoveryOptionResult:
    """
    Reroute connection passengers to alternative flights.
    Costs:
      - Rebooking admin cost (USD 75 per connection PAX).
      - Hotel accommodation if delay > 4 hours (USD 120 per PAX).
      - Partial compensation at 0.5× rate (disruption acknowledged but PAX saved).
    Only meaningful when there are connection passengers.
    """
    conn_pax = req.connection_passengers
    delay_hours = req.delay_minutes / 60.0

    rebooking_cost = conn_pax * 75.0
    hotel_cost = conn_pax * 120.0 if delay_hours > 4.0 else 0.0
    pax_compensation = conn_pax * req.passenger_compensation_rate * 0.5

    total_cost = rebooking_cost + hotel_cost + pax_compensation
    feasible = conn_pax > 0

    return RecoveryOptionResult(
        option_type="PASSENGER_REROUTING",
        description=(
            f"Reroute {conn_pax} connection passenger(s) to alternative flights/airlines. "
            f"{'Hotel provided (delay >4 h).' if hotel_cost > 0 else ''}"
        ),
        total_cost=round(total_cost, 2),
        crew_overtime_cost=0.0,
        fuel_cost=0.0,
        passenger_compensation_cost=round(pax_compensation, 2),
        slot_penalty_cost=0.0,
        mro_cost=round(rebooking_cost + hotel_cost, 2),
        estimated_delay_reduction=req.delay_minutes // 2,
        feasible=feasible,
        feasibility_notes=(
            f"{conn_pax} connection passengers eligible for rerouting."
            if feasible
            else "No connection passengers; rerouting not applicable."
        ),
        generated_by="MILP",
        passenger_impact_score=passenger_impact_score(
            "PASSENGER_REROUTING", req.delay_minutes, req.delay_minutes // 2, req.passenger_count, req.connection_passengers
        ),
        crew_duty_compliance="COMPLIANT",
        regulatory_feasible=True,
    )


# ---------------------------------------------------------------------------
# MILP optimiser
# ---------------------------------------------------------------------------

class MILPOptimiser:
    """
    Generates all candidate recovery options, evaluates their costs, then
    runs a PuLP integer programme to formally identify the minimum-cost feasible
    option.  All options are returned ranked by ascending total cost.
    """

    def generate_options(self, req: OptimisationRequest) -> List[RecoveryOptionResult]:
        candidates: List[RecoveryOptionResult] = []

        # --- 1. Delay Absorption ---
        candidates.append(_build_delay_absorption(req))

        # --- 2. Flight Cancellation ---
        candidates.append(_build_flight_cancellation(req))

        # --- 3. Aircraft Swap (one option per spare aircraft) ---
        for spare in req.available_aircraft:
            candidates.append(_build_aircraft_swap(req, spare))

        # --- 4. Crew Substitution ---
        candidates.append(_build_crew_substitution(req))

        # --- 5. Passenger Rerouting ---
        candidates.append(_build_passenger_rerouting(req))

        # ---------------------------------------------------------------
        # MILP: binary variable per option, minimise cost, pick best
        # ---------------------------------------------------------------
        feasible_candidates = [c for c in candidates if c.feasible]

        if feasible_candidates:
            prob = pulp.LpProblem("delay_recovery_minimise_cost", pulp.LpMinimize)

            x = [
                pulp.LpVariable(f"x_{i}", cat="Binary")
                for i in range(len(feasible_candidates))
            ]

            # Objective: minimise total cost across selected options
            prob += pulp.lpSum(
                x[i] * feasible_candidates[i].total_cost
                for i in range(len(feasible_candidates))
            )

            # Constraint: select exactly one option
            prob += pulp.lpSum(x) == 1

            # Solve with CBC (bundled with PuLP)
            solver = pulp.PULP_CBC_CMD(msg=False)
            status = prob.solve(solver)

            if pulp.LpStatus[status] == "Optimal":
                for i, xi in enumerate(x):
                    if pulp.value(xi) == 1:
                        feasible_candidates[i].description += (
                            " [★ MILP Optimal Selection]"
                        )
                        break
                logger.info("MILP solved: optimal cost = %.2f", pulp.value(prob.objective))
            else:
                logger.warning("MILP solver status: %s", pulp.LpStatus[status])

        # Sort all options by total cost (ascending); rank assigned in main.py
        candidates.sort(key=lambda c: (not c.feasible, c.total_cost))
        return candidates
