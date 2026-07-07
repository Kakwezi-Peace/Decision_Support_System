package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rw.ac.dss.model.Flight;

import java.time.LocalDateTime;

@Data
public class CreateFlightRequest {

    @NotBlank
    private String flightNumber;

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDateTime scheduledDeparture;

    @NotNull
    private LocalDateTime scheduledArrival;

    @NotNull
    private Long aircraftId;

    private Flight.FlightStatus status;

    private int passengerCount;

    private int availableSeats;

    private double fuelCost;

    private String notes;
}
