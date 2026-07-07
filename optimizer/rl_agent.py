"""
rl_agent.py
Q-Learning agent for adaptive aircraft delay recovery recommendations at RwandAir.

State space  : (delay_category, delay_severity, crew_availability, spare_aircraft_available)
Action space : [DELAY_ABSORPTION, FLIGHT_CANCELLATION, AIRCRAFT_SWAP,
                CREW_SUBSTITUTION, PASSENGER_REROUTING]

The Q-table is pre-seeded with aviation domain knowledge and can be further
updated online via the update() method if the service records outcome feedback.
"""

from __future__ import annotations

import logging
from typing import Dict, List, Tuple

import numpy as np

from models import DelayCategory, OptimisationRequest, RecoveryOptionResult
from option_scoring import crew_duty_status, passenger_impact_score

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# State & Action definitions
# ---------------------------------------------------------------------------

# Delay severity buckets
SEVERITY_LOW = 0       # < 30 min
SEVERITY_MEDIUM = 1    # 30–120 min
SEVERITY_HIGH = 2      # > 120 min

# Crew availability buckets
CREW_GOOD = 0      # ≥ 2 crew with >2 h duty remaining
CREW_LIMITED = 1   # exactly 1 crew with >2 h remaining
CREW_NONE = 2      # no crew with >2 h remaining

# Spare aircraft
SPARE_YES = 0
SPARE_NO = 1

ACTIONS: List[str] = [
    "DELAY_ABSORPTION",
    "FLIGHT_CANCELLATION",
    "AIRCRAFT_SWAP",
    "CREW_SUBSTITUTION",
    "PASSENGER_REROUTING",
]

NUM_CATEGORIES = len(DelayCategory)       # 7
NUM_SEVERITIES = 3
NUM_CREW_STATES = 3
NUM_SPARE_STATES = 2
NUM_ACTIONS = len(ACTIONS)

STATE_SHAPE: Tuple[int, ...] = (
    NUM_CATEGORIES,
    NUM_SEVERITIES,
    NUM_CREW_STATES,
    NUM_SPARE_STATES,
)

# ---------------------------------------------------------------------------
# Delay category → integer index mapping
# ---------------------------------------------------------------------------
CATEGORY_INDEX: Dict[str, int] = {
    DelayCategory.TECHNICAL: 0,
    DelayCategory.WEATHER: 1,
    DelayCategory.ATC: 2,
    DelayCategory.CREW: 3,
    DelayCategory.COMMERCIAL: 4,
    DelayCategory.REACTIONARY: 5,
    DelayCategory.OTHER: 6,
}


# ---------------------------------------------------------------------------
# Q-table pre-seeding helper
# ---------------------------------------------------------------------------

def _build_initial_q_table() -> np.ndarray:
    """
    Build a Q-table pre-seeded with domain knowledge from aviation recovery
    best practices.  Higher Q-value = better action for that state.

    Rules encoded:
      - LOW delay → DELAY_ABSORPTION is almost always cheapest.
      - TECHNICAL + HIGH delay + spare aircraft available → AIRCRAFT_SWAP.
      - TECHNICAL + HIGH delay + no spare → FLIGHT_CANCELLATION.
      - WEATHER (any severity) → DELAY_ABSORPTION first, CANCELLATION second.
      - ATC delay → DELAY_ABSORPTION (usually short and mandatory).
      - CREW delay → CREW_SUBSTITUTION when crew available, else CANCELLATION.
      - COMMERCIAL delay → PASSENGER_REROUTING for connections.
      - REACTIONARY + MEDIUM/HIGH → AIRCRAFT_SWAP if spare, else ABSORPTION.
    """
    # Shape: (category, severity, crew, spare, action)
    q = np.zeros((*STATE_SHAPE, NUM_ACTIONS), dtype=np.float32)

    # Action indices for readability
    A_ABS = 0   # DELAY_ABSORPTION
    A_CAN = 1   # FLIGHT_CANCELLATION
    A_SWP = 2   # AIRCRAFT_SWAP
    A_CRW = 3   # CREW_SUBSTITUTION
    A_RRT = 4   # PASSENGER_REROUTING

    for cat in range(NUM_CATEGORIES):
        for sev in range(NUM_SEVERITIES):
            for crew in range(NUM_CREW_STATES):
                for spare in range(NUM_SPARE_STATES):

                    base = q[cat, sev, crew, spare]

                    # ── LOW delay: absorption is king ──────────────────────
                    if sev == SEVERITY_LOW:
                        base[A_ABS] = 9.0
                        base[A_CRW] = 5.0 if crew == CREW_GOOD else 2.0
                        base[A_RRT] = 4.0
                        base[A_CAN] = 0.5
                        base[A_SWP] = 1.0

                    # ── MEDIUM delay ───────────────────────────────────────
                    elif sev == SEVERITY_MEDIUM:
                        base[A_ABS] = 5.0
                        base[A_RRT] = 4.5
                        base[A_CRW] = 6.0 if crew != CREW_NONE else 1.0
                        base[A_SWP] = 7.0 if spare == SPARE_YES else 2.0
                        base[A_CAN] = 1.5

                    # ── HIGH delay ─────────────────────────────────────────
                    else:  # SEVERITY_HIGH
                        base[A_ABS] = 3.0
                        base[A_SWP] = 8.5 if spare == SPARE_YES else 2.0
                        base[A_CRW] = 6.5 if crew == CREW_GOOD else 2.0
                        base[A_RRT] = 5.0
                        base[A_CAN] = 4.0 if spare == SPARE_NO else 2.0

                    # ── Category-specific overrides ───────────────────────

                    # TECHNICAL
                    if cat == CATEGORY_INDEX[DelayCategory.TECHNICAL]:
                        if sev == SEVERITY_HIGH and spare == SPARE_YES:
                            base[A_SWP] = 10.0  # strong preference
                            base[A_ABS] = 1.0
                        if sev == SEVERITY_HIGH and spare == SPARE_NO:
                            base[A_CAN] = 7.0
                            base[A_ABS] = 1.0

                    # WEATHER – no spare aircraft helps; absorb or cancel
                    elif cat == CATEGORY_INDEX[DelayCategory.WEATHER]:
                        base[A_ABS] = max(base[A_ABS], 7.0)
                        base[A_SWP] = 1.0   # weather affects all aircraft
                        base[A_CAN] = 5.0 if sev == SEVERITY_HIGH else 3.0

                    # ATC – mandatory hold; absorption only real option
                    elif cat == CATEGORY_INDEX[DelayCategory.ATC]:
                        base[A_ABS] = 10.0
                        base[A_SWP] = 0.5
                        base[A_CAN] = 0.5

                    # CREW – substitute if possible
                    elif cat == CATEGORY_INDEX[DelayCategory.CREW]:
                        if crew != CREW_NONE:
                            base[A_CRW] = 10.0
                            base[A_ABS] = 2.0
                        else:
                            base[A_CAN] = 7.0
                            base[A_ABS] = 3.0

                    # COMMERCIAL – reroute connections; absorption feasible
                    elif cat == CATEGORY_INDEX[DelayCategory.COMMERCIAL]:
                        base[A_RRT] = 8.0
                        base[A_ABS] = 6.0

                    # REACTIONARY – swap if spare, else absorb
                    elif cat == CATEGORY_INDEX[DelayCategory.REACTIONARY]:
                        if spare == SPARE_YES and sev != SEVERITY_LOW:
                            base[A_SWP] = 8.0
                        else:
                            base[A_ABS] = max(base[A_ABS], 6.0)

                    # OTHER – generic preference order
                    else:
                        base[A_ABS] = max(base[A_ABS], 5.0)

    return q


# ---------------------------------------------------------------------------
# Q-Learning agent
# ---------------------------------------------------------------------------

class QAgent:
    """
    Q-Learning agent pre-seeded with aviation domain knowledge.

    The Q-table can be updated online by calling update() with observed
    reward signals (e.g. actual cost savings vs baseline).
    """

    ALPHA = 0.1    # learning rate
    GAMMA = 0.9    # discount factor
    EPSILON = 0.05 # exploration rate (low; mostly exploit pre-trained policy)

    def __init__(self) -> None:
        self.q_table: np.ndarray = _build_initial_q_table()
        self.action_labels: List[str] = ACTIONS
        logger.info("QAgent initialised with domain-seeded Q-table.")

    # ------------------------------------------------------------------
    # State extraction
    # ------------------------------------------------------------------

    def get_state(self, req: OptimisationRequest) -> Tuple[int, int, int, int]:
        """Convert an OptimisationRequest into a discrete state tuple."""

        # Delay severity
        if req.delay_minutes < 30:
            severity = SEVERITY_LOW
        elif req.delay_minutes <= 120:
            severity = SEVERITY_MEDIUM
        else:
            severity = SEVERITY_HIGH

        # Crew availability (count crew with >2 h duty remaining)
        well_rested = sum(
            1
            for c in req.available_crew
            if float(c.get("duty_hours_remaining", 0.0)) > 2.0
        )
        if well_rested >= 2:
            crew_state = CREW_GOOD
        elif well_rested == 1:
            crew_state = CREW_LIMITED
        else:
            crew_state = CREW_NONE

        # Spare aircraft
        serviceable_spares = [
            a
            for a in req.available_aircraft
            if str(a.get("status", "SERVICEABLE")).upper() == "SERVICEABLE"
            and int(a.get("seats", 0)) >= req.passenger_count
        ]
        spare_state = SPARE_YES if serviceable_spares else SPARE_NO

        cat_idx = CATEGORY_INDEX.get(req.delay_category, 6)

        return cat_idx, severity, crew_state, spare_state

    # ------------------------------------------------------------------
    # Recommendation
    # ------------------------------------------------------------------

    def get_recommendation(self, req: OptimisationRequest) -> RecoveryOptionResult:
        """
        Return a RecoveryOptionResult based on the Q-table's greedy action
        for the derived state.  Cost estimates mirror the MILP option builders
        so the result can be directly compared.
        """
        state = self.get_state(req)
        q_values = self.q_table[state]

        best_action_idx: int = int(np.argmax(q_values))
        best_action: str = self.action_labels[best_action_idx]

        logger.info(
            "RL state=%s → action=%s (Q=%.3f)", state, best_action, q_values[best_action_idx]
        )

        return self._build_rl_option(req, best_action, state, q_values)

    # ------------------------------------------------------------------
    # RL option construction (cost estimates reuse the same formulas)
    # ------------------------------------------------------------------

    def _build_rl_option(
        self,
        req: OptimisationRequest,
        action: str,
        state: Tuple[int, int, int, int],
        q_values: np.ndarray,
    ) -> RecoveryOptionResult:
        """Build a RecoveryOptionResult for the RL-chosen action."""

        delay_hours = req.delay_minutes / 60.0

        # ── DELAY_ABSORPTION ───────────────────────────────────────────
        if action == "DELAY_ABSORPTION":
            crew_ot = sum(
                max(0.0, delay_hours - float(c.get("duty_hours_remaining", 4.0)))
                * float(c.get("overtime_rate", 80.0))
                for c in req.available_crew
            )
            slot = delay_hours * req.slot_penalty_per_hour
            pax_comp = req.connection_passengers * req.passenger_compensation_rate
            total = crew_ot + slot + pax_comp
            crew_duty_compliance, regulatory_feasible = crew_duty_status(req.available_crew, delay_hours, exempt=False)
            return RecoveryOptionResult(
                option_type="DELAY_ABSORPTION",
                description=(
                    f"[RL Recommendation] Absorb {req.delay_minutes}-min delay. "
                    f"Q-values: {np.round(q_values, 2).tolist()}"
                ),
                total_cost=round(total, 2),
                crew_overtime_cost=round(crew_ot, 2),
                fuel_cost=0.0,
                passenger_compensation_cost=round(pax_comp, 2),
                slot_penalty_cost=round(slot, 2),
                mro_cost=0.0,
                estimated_delay_reduction=0,
                feasible=True,
                feasibility_notes="RL agent recommends delay absorption based on learned policy.",
                generated_by="RL",
                passenger_impact_score=passenger_impact_score(
                    "DELAY_ABSORPTION", req.delay_minutes, 0, req.passenger_count, req.connection_passengers
                ),
                crew_duty_compliance=crew_duty_compliance,
                regulatory_feasible=regulatory_feasible,
            )

        # ── FLIGHT_CANCELLATION ────────────────────────────────────────
        elif action == "FLIGHT_CANCELLATION":
            pax_comp = req.passenger_count * req.passenger_compensation_rate * 1.5
            rev_loss = req.passenger_count * 350.0
            total = pax_comp + rev_loss
            return RecoveryOptionResult(
                option_type="FLIGHT_CANCELLATION",
                description=(
                    f"[RL Recommendation] Cancel flight {req.flight_number}. "
                    f"Q-values: {np.round(q_values, 2).tolist()}"
                ),
                total_cost=round(total, 2),
                crew_overtime_cost=0.0,
                fuel_cost=0.0,
                passenger_compensation_cost=round(pax_comp, 2),
                slot_penalty_cost=0.0,
                mro_cost=round(rev_loss, 2),
                estimated_delay_reduction=req.delay_minutes,
                feasible=True,
                feasibility_notes="RL agent recommends cancellation based on learned policy.",
                generated_by="RL",
                passenger_impact_score=passenger_impact_score(
                    "FLIGHT_CANCELLATION", req.delay_minutes, req.delay_minutes, req.passenger_count, req.connection_passengers
                ),
                crew_duty_compliance="COMPLIANT",
                regulatory_feasible=True,
            )

        # ── AIRCRAFT_SWAP ──────────────────────────────────────────────
        elif action == "AIRCRAFT_SWAP":
            serviceable = [
                a
                for a in req.available_aircraft
                if str(a.get("status", "SERVICEABLE")).upper() == "SERVICEABLE"
                and int(a.get("seats", 0)) >= req.passenger_count
            ]
            if not serviceable:
                # Fallback to absorption if no spare available
                return self._build_rl_option(req, "DELAY_ABSORPTION", state, q_values)

            spare = serviceable[0]
            spare_location = str(spare.get("location", "KGL"))
            ferry_h = max(abs(
                (lambda loc: {
                    "KGL": 0.0, "NBO": 1.5, "EBB": 0.8, "DAR": 1.8,
                    "JRO": 1.9, "ADD": 2.0, "LOS": 3.5, "JNB": 4.0,
                    "CDG": 8.5, "DXB": 5.0,
                }.get(loc.upper(), 2.5))(spare_location)
                - (lambda loc: {
                    "KGL": 0.0, "NBO": 1.5, "EBB": 0.8, "DAR": 1.8,
                    "JRO": 1.9, "ADD": 2.0, "LOS": 3.5, "JNB": 4.0,
                    "CDG": 8.5, "DXB": 5.0,
                }.get(loc.upper(), 2.5))(req.origin)
            ), 0.5)
            fuel = ferry_h * req.fuel_cost_per_hour
            mro = 500.0 if float(spare.get("last_maintenance_hours", 0.0)) < 10 else 0.0
            swap_delay_h = ferry_h
            pax_comp = (
                req.connection_passengers
                * req.passenger_compensation_rate
                * (swap_delay_h / (delay_hours + 0.001))
            )
            total = fuel + mro + pax_comp
            delay_reduction = max(0, req.delay_minutes - int(ferry_h * 60))
            return RecoveryOptionResult(
                option_type="AIRCRAFT_SWAP",
                description=(
                    f"[RL Recommendation] Swap in aircraft {spare.get('id','?')} "
                    f"from {spare_location}. Q-values: {np.round(q_values, 2).tolist()}"
                ),
                total_cost=round(total, 2),
                crew_overtime_cost=0.0,
                fuel_cost=round(fuel, 2),
                passenger_compensation_cost=round(pax_comp, 2),
                slot_penalty_cost=0.0,
                mro_cost=round(mro, 2),
                estimated_delay_reduction=delay_reduction,
                feasible=True,
                feasibility_notes="RL agent recommends aircraft swap based on learned policy.",
                generated_by="RL",
                passenger_impact_score=passenger_impact_score(
                    "AIRCRAFT_SWAP", req.delay_minutes, delay_reduction, req.passenger_count, req.connection_passengers
                ),
                crew_duty_compliance="COMPLIANT",
                regulatory_feasible=True,
            )

        # ── CREW_SUBSTITUTION ──────────────────────────────────────────
        elif action == "CREW_SUBSTITUTION":
            replacements = [
                c
                for c in req.available_crew
                if float(c.get("duty_hours_remaining", 0.0)) >= 3.0
            ]
            near_limit = [
                c for c in req.available_crew
                if float(c.get("duty_hours_remaining", 10.0)) < 1.0
            ]
            feasible = len(replacements) > 0
            n_sub = min(len(near_limit), len(replacements))
            transport = n_sub * 200.0
            slot = 0.5 * req.slot_penalty_per_hour
            pax_comp = (
                req.connection_passengers * req.passenger_compensation_rate
                * (0.5 / (delay_hours + 0.001))
            )
            total = max(transport + slot + pax_comp, 0.0)
            return RecoveryOptionResult(
                option_type="CREW_SUBSTITUTION",
                description=(
                    f"[RL Recommendation] Substitute {n_sub} crew member(s). "
                    f"Q-values: {np.round(q_values, 2).tolist()}"
                ),
                total_cost=round(total, 2),
                crew_overtime_cost=round(transport, 2),
                fuel_cost=0.0,
                passenger_compensation_cost=round(pax_comp, 2),
                slot_penalty_cost=round(slot, 2),
                mro_cost=0.0,
                estimated_delay_reduction=max(0, req.delay_minutes - 30),
                feasible=feasible,
                feasibility_notes=(
                    f"{len(replacements)} replacement(s) available."
                    if feasible
                    else "No qualified crew for substitution."
                ),
                generated_by="RL",
                passenger_impact_score=passenger_impact_score(
                    "CREW_SUBSTITUTION", req.delay_minutes, max(0, req.delay_minutes - 30), req.passenger_count, req.connection_passengers
                ),
                crew_duty_compliance="COMPLIANT" if feasible else "VIOLATION",
                regulatory_feasible=feasible,
            )

        # ── PASSENGER_REROUTING ────────────────────────────────────────
        else:  # PASSENGER_REROUTING
            conn_pax = req.connection_passengers
            rebooking = conn_pax * 75.0
            hotel = conn_pax * 120.0 if delay_hours > 4.0 else 0.0
            pax_comp = conn_pax * req.passenger_compensation_rate * 0.5
            total = rebooking + hotel + pax_comp
            feasible = conn_pax > 0
            return RecoveryOptionResult(
                option_type="PASSENGER_REROUTING",
                description=(
                    f"[RL Recommendation] Reroute {conn_pax} connection passenger(s). "
                    f"Q-values: {np.round(q_values, 2).tolist()}"
                ),
                total_cost=round(total, 2),
                crew_overtime_cost=0.0,
                fuel_cost=0.0,
                passenger_compensation_cost=round(pax_comp, 2),
                slot_penalty_cost=0.0,
                mro_cost=round(rebooking + hotel, 2),
                estimated_delay_reduction=req.delay_minutes // 2,
                feasible=feasible,
                feasibility_notes=(
                    f"Rerouting {conn_pax} passengers."
                    if feasible
                    else "No connection passengers; rerouting not applicable."
                ),
                generated_by="RL",
                passenger_impact_score=passenger_impact_score(
                    "PASSENGER_REROUTING", req.delay_minutes, req.delay_minutes // 2, req.passenger_count, req.connection_passengers
                ),
                crew_duty_compliance="COMPLIANT",
                regulatory_feasible=True,
            )

    # ------------------------------------------------------------------
    # Online Q-table update (Bellman equation)
    # ------------------------------------------------------------------

    def update(
        self,
        state: Tuple[int, int, int, int],
        action_idx: int,
        reward: float,
        next_state: Tuple[int, int, int, int],
    ) -> None:
        """
        Update the Q-table using the Bellman equation.
        reward: positive = cost saved vs doing nothing; negative = extra cost incurred.
        """
        current_q = self.q_table[state][action_idx]
        max_future_q = float(np.max(self.q_table[next_state]))
        new_q = current_q + self.ALPHA * (reward + self.GAMMA * max_future_q - current_q)
        self.q_table[state][action_idx] = new_q
        logger.debug(
            "Q-table updated: state=%s action=%d Q: %.3f → %.3f",
            state, action_idx, current_q, new_q,
        )
