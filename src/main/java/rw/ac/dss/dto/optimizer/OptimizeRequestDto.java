package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mirrors optimizer/models.py OptimisationRequest. Field names map to the
 * snake_case JSON the FastAPI service expects.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizeRequestDto {

    @JsonProperty("delay_event_id")
    private Long delayEventId;

    @JsonProperty("aircraft_id")
    private Long aircraftId;

    @JsonProperty("flight_number")
    private String flightNumber;

    private String origin;

    private String destination;

    @JsonProperty("delay_minutes")
    private int delayMinutes;

    @JsonProperty("delay_category")
    private String delayCategory;

    @JsonProperty("passenger_count")
    private int passengerCount;

    @JsonProperty("connection_passengers")
    private int connectionPassengers;

    @JsonProperty("available_aircraft")
    private List<AircraftSpareDto> availableAircraft;

    @JsonProperty("available_crew")
    private List<CrewAvailabilityDto> availableCrew;

    @JsonProperty("fuel_cost_per_hour")
    private double fuelCostPerHour;

    @JsonProperty("slot_penalty_per_hour")
    private double slotPenaltyPerHour;

    @JsonProperty("passenger_compensation_rate")
    private double passengerCompensationRate;

    @JsonProperty("aircraft_type")
    private String aircraftType;
}
