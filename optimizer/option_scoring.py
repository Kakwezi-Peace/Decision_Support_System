"""
option_scoring.py
Heuristic scoring for the three option-characterisation fields the proposal names
in Section 2.2.4 but does not give a formula for: "passenger impact score, crew
duty compliance status, and regulatory feasibility." No such formula exists in the
research proposal, so these are original, documented heuristics grounded in the
same request data (duty_hours_remaining, connection_passengers, etc.) already used
for cost calculations elsewhere in this engine - not values fabricated from thin air.

passenger_impact_score: 0-100, higher = worse passenger disruption.
  - Cancellation always disrupts every passenger, independent of "delay minutes",
    so it gets a high floor rather than being scored on residual delay.
  - Every other option is scored on (a) how much delay is left after applying the
    option and (b) what share of passengers are on tight connections.

crew_duty_status: COMPLIANT / AT_RISK / VIOLATION, based on whether the crew
  actually operating the flight under this option would exceed their legal duty
  hours. Options that release or replace the affected crew (cancellation, aircraft
  swap, crew substitution) are exempt from this check by construction - the
  original at-risk crew never end up flying.
"""

from __future__ import annotations

from typing import Dict, List, Tuple


def passenger_impact_score(
    option_type: str,
    delay_minutes: int,
    estimated_delay_reduction: int,
    passenger_count: int,
    connection_passengers: int,
) -> float:
    if option_type == "FLIGHT_CANCELLATION":
        connection_ratio = connection_passengers / max(passenger_count, 1)
        return round(min(100.0, 70.0 + connection_ratio * 30.0), 1)

    residual_delay = max(delay_minutes - estimated_delay_reduction, 0)
    delay_component = min(60.0, (residual_delay / 240.0) * 60.0)  # 4h+ residual delay maxes this out
    connection_ratio = connection_passengers / max(passenger_count, 1)
    connection_component = min(40.0, connection_ratio * 100.0)  # an all-connections flight maxes this out
    return round(min(100.0, delay_component + connection_component), 1)


def crew_duty_status(
    available_crew: List[Dict],
    delay_hours: float,
    exempt: bool = False,
) -> Tuple[str, bool]:
    """
    Returns (crew_duty_compliance, regulatory_feasible).
    exempt=True for options where the crew whose duty clock this delay would run
    down never actually fly the extra hours (swap, cancellation, substitution).
    """
    if exempt:
        return "COMPLIANT", True

    violations = [c for c in available_crew if float(c.get("duty_hours_remaining", 4.0)) <= 0.0]
    near_limit = [
        c for c in available_crew if 0.0 < float(c.get("duty_hours_remaining", 4.0)) < delay_hours
    ]
    if violations:
        return "VIOLATION", False
    if near_limit:
        return "AT_RISK", True
    return "COMPLIANT", True
