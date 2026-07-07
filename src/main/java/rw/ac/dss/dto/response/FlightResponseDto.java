package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.Flight;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightResponseDto {

    private Long id;
    private String flightNumber;
    private String origin;
    private String destination;
    private LocalDateTime scheduledDeparture;
    private LocalDateTime scheduledArrival;
    private Flight.FlightStatus status;
    private Long aircraftId;
    private String aircraftRegistration;
    private int passengerCount;
    private int availableSeats;

    public static FlightResponseDto from(Flight f) {
        return FlightResponseDto.builder()
                .id(f.getId())
                .flightNumber(f.getFlightNumber())
                .origin(f.getOrigin())
                .destination(f.getDestination())
                .scheduledDeparture(f.getScheduledDeparture())
                .scheduledArrival(f.getScheduledArrival())
                .status(f.getStatus())
                .aircraftId(f.getAircraft() != null ? f.getAircraft().getId() : null)
                .aircraftRegistration(f.getAircraft() != null ? f.getAircraft().getRegistrationNumber() : null)
                .passengerCount(f.getPassengerCount())
                .availableSeats(f.getAvailableSeats())
                .build();
    }
}
