export type Role =
  | "ADMIN"
  | "OPERATIONS_CONTROLLER"
  | "CREW_SCHEDULER"
  | "MAINTENANCE_CONTROLLER"
  | "COMMERCIAL_SERVICES"
  | "SENIOR_MANAGEMENT";

export interface AuthResponse {
  token: string;
  username: string;
  fullName: string;
  role: Role;
}

export type AircraftType = "BOEING_737" | "AIRBUS_A330" | "TURBOPROP_Q400";
export type AircraftStatus = "SERVICEABLE" | "UNSERVICEABLE" | "IN_MAINTENANCE";

export interface Aircraft {
  id: number;
  registrationNumber: string;
  aircraftType: AircraftType;
  status: AircraftStatus;
  currentLocation: string;
  totalSeats: number;
  yearManufactured: number;
  lastMaintenanceDate: string | null;
}

export type CrewRole = "CAPTAIN" | "FIRST_OFFICER" | "SENIOR_FA" | "FLIGHT_ATTENDANT";
export type CrewStatus = "AVAILABLE" | "ON_DUTY" | "ON_REST" | "ON_LEAVE" | "SICK";

export interface Crew {
  id: number;
  firstName: string;
  lastName: string;
  employeeId: string;
  role: CrewRole;
  qualifications: string;
  dutyHoursUsed: number;
  maxDutyHours: number;
  currentLocation: string | null;
  status: CrewStatus;
  overtimeRate: number;
}

export type FlightStatus = "SCHEDULED" | "DELAYED" | "IN_FLIGHT" | "ARRIVED" | "CANCELLED" | "DIVERTED";

export interface Flight {
  id: number;
  flightNumber: string;
  origin: string;
  destination: string;
  scheduledDeparture: string;
  scheduledArrival: string;
  status: FlightStatus;
  aircraftId: number;
  aircraftRegistration: string;
  passengerCount: number;
  availableSeats: number;
}

export type DelayCategory = "TECHNICAL" | "WEATHER" | "ATC" | "CREW" | "COMMERCIAL" | "REACTIONARY" | "OTHER";
export type DelayStatus = "ACTIVE" | "RECOVERING" | "RESOLVED";

export interface DelayEvent {
  id: number;
  flightId: number;
  flightNumber: string;
  delayCategory: DelayCategory;
  delayMinutes: number;
  delayCause: string | null;
  reportedBy: string | null;
  status: DelayStatus;
  reportedAt: string;
}

export type OptionType =
  | "DELAY_ABSORPTION"
  | "FLIGHT_CANCELLATION"
  | "AIRCRAFT_SWAP"
  | "CREW_SUBSTITUTION"
  | "PASSENGER_REROUTING"
  | "COMBINED";
export type GeneratedBy = "MILP" | "RL" | "MANUAL";

export type CrewDutyCompliance = "COMPLIANT" | "AT_RISK" | "VIOLATION" | "UNKNOWN";

export interface RecoveryOption {
  id: number;
  optionType: OptionType;
  description: string;
  rank: number;
  totalCost: number;
  crewOvertimeCost: number;
  fuelCost: number;
  passengerCompensationCost: number;
  slotPenaltyCost: number;
  mroCost: number;
  estimatedDelayReduction: number;
  feasible: boolean;
  feasibilityNotes: string;
  selected: boolean;
  generatedBy: GeneratedBy;
  passengerImpactScore: number | null;
  crewDutyCompliance: CrewDutyCompliance;
  regulatoryFeasible: boolean | null;
}

export type ScenarioStatus = "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
export type ScenarioPriority = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";

export interface RecoveryScenario {
  id: number;
  delayEventId: number;
  scenarioTitle: string;
  status: ScenarioStatus;
  priority: ScenarioPriority;
  optimisationRunAt: string | null;
  selectedOptionId: number | null;
  decisionMadeBy: string | null;
  resolvedAt: string | null;
  computationTimeMs: number;
  options: RecoveryOption[];
  actualCost: number | null;
  actualCostSaved: number | null;
  manualBaselineCost: number | null;
  rlPolicyUpdated: boolean | null;
  rlFeedbackMessage: string | null;
}

export interface RecoveryScenarioSummary {
  id: number;
  delayEventId: number;
  flightNumber: string | null;
  origin: string | null;
  destination: string | null;
  delayCategory: DelayCategory | null;
  delayMinutes: number;
  status: ScenarioStatus;
  priority: ScenarioPriority;
  optimisationRunAt: string | null;
  resolvedAt: string | null;
  decisionMadeBy: string | null;
  selectedOptionType: OptionType | null;
  selectedOptionCost: number | null;
  outcomeRecorded: boolean;
  actualCost: number | null;
  actualCostSaved: number | null;
  manualBaselineCost: number | null;
}

export interface AnalyticsSummary {
  totalDelayEvents: number;
  totalResolvedScenarios: number;
  totalPendingScenarios: number;
  delayCountByCategory: Record<string, number>;
  avgDelayMinutesByCategory: Record<string, number>;
  costByCategory: Record<string, number>;
  totalCostSaved: number;
  totalEstimatedCostOfSelectedOptions: number;
  totalActualCost: number;
  avgComputationTimeMs: number | null;
  avgDecisionMinutes: number | null;
  manualBaselineTotal: number | null;
  actualCostForBaselineComparison: number | null;
  manualVsDssSavingsPercent: number | null;
  scenariosWithManualBaseline: number;
}

export type TicketClass = "ECONOMY" | "BUSINESS" | "FIRST";
export type PassengerStatus = "CHECKED_IN" | "BOARDED" | "DISRUPTED" | "REACCOMMODATED";

export interface Passenger {
  id: number;
  flightId: number;
  flightNumber: string;
  firstName: string;
  lastName: string;
  bookingReference: string;
  seatNumber: string | null;
  ticketClass: TicketClass;
  hasConnection: boolean;
  connectionFlight: string | null;
  connectionDeadlineMinutes: number;
  reAccommodated: boolean;
  compensationPaid: number;
  status: PassengerStatus;
  createdAt: string;
}

export interface ApiError {
  status: number;
  error: string;
  message: string;
}
