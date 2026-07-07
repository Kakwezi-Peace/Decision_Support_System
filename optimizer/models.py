from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from enum import Enum


class DelayCategory(str, Enum):
    TECHNICAL = "TECHNICAL"
    WEATHER = "WEATHER"
    ATC = "ATC"
    CREW = "CREW"
    COMMERCIAL = "COMMERCIAL"
    REACTIONARY = "REACTIONARY"
    OTHER = "OTHER"


class AircraftInfo(BaseModel):
    id: int
    type: str
    location: str
    seats: int
    status: Optional[str] = "SERVICEABLE"
    last_maintenance_hours: Optional[float] = 0.0


class CrewMember(BaseModel):
    id: int
    role: str                         # CAPTAIN, FIRST_OFFICER, CABIN_CREW
    qualification: str                # Aircraft type qualification e.g. B737, A330
    duty_hours_remaining: float       # Hours remaining in legal duty period
    overtime_rate: float              # USD/hour for overtime


class OptimisationRequest(BaseModel):
    delay_event_id: int
    aircraft_id: int
    flight_number: str
    origin: str
    destination: str
    delay_minutes: int
    delay_category: DelayCategory
    passenger_count: int
    connection_passengers: int
    available_aircraft: List[Dict[str, Any]] = Field(
        default_factory=list,
        description="List of spare aircraft dicts with keys: id, type, location, seats, status, last_maintenance_hours"
    )
    available_crew: List[Dict[str, Any]] = Field(
        default_factory=list,
        description="List of crew dicts with keys: id, role, qualification, duty_hours_remaining, overtime_rate"
    )
    fuel_cost_per_hour: float = Field(default=4500.0, description="Fuel cost in USD per flight hour")
    slot_penalty_per_hour: float = Field(default=800.0, description="ATC slot penalty in USD per hour of delay")
    passenger_compensation_rate: float = Field(
        default=150.0, description="Compensation in USD per disrupted passenger"
    )
    aircraft_type: Optional[str] = Field(default="B737", description="Type of the delayed aircraft")


class RecoveryOptionResult(BaseModel):
    option_type: str
    description: str
    rank: int = 0
    total_cost: float
    crew_overtime_cost: float
    fuel_cost: float
    passenger_compensation_cost: float
    slot_penalty_cost: float
    mro_cost: float
    estimated_delay_reduction: int   # minutes saved compared to doing nothing
    feasible: bool
    feasibility_notes: str
    generated_by: str                # "MILP" or "RL"

    # Heuristic option-characterisation fields (see option_scoring.py for the
    # formulas and why they're heuristics rather than a proposal-specified model).
    passenger_impact_score: float = 0.0     # 0-100, higher = worse passenger disruption
    crew_duty_compliance: str = "COMPLIANT"  # COMPLIANT | AT_RISK | VIOLATION
    regulatory_feasible: bool = True


class OptimisationResponse(BaseModel):
    delay_event_id: int
    options: List[RecoveryOptionResult]
    computation_time_ms: float
    recommended_option_index: int


class FeedbackRequest(BaseModel):
    """
    Closes the proposal's "Feedback and Learning Loop" (Section 2.2.4): once a
    recovery decision has actually played out, the OCC reports what it really cost
    so the RL policy can be refined from real experience rather than staying frozen
    at its domain-seeded initial values.
    """

    optimisation_request: OptimisationRequest
    chosen_action: str = Field(
        description="One of the RL agent's action labels, e.g. AIRCRAFT_SWAP, that was actually implemented"
    )
    estimated_cost: float = Field(description="The chosen option's estimated total cost at decision time")
    actual_cost: float = Field(description="What the recovery action actually cost once implemented")


class FeedbackResponse(BaseModel):
    updated: bool
    reward: float
    old_q_value: Optional[float] = None
    new_q_value: Optional[float] = None
    message: str
