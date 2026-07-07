package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.Passenger;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PassengerResponseDto {

    private Long id;
    private Long flightId;
    private String flightNumber;
    private String firstName;
    private String lastName;
    private String bookingReference;
    private String seatNumber;
    private Passenger.TicketClass ticketClass;
    private boolean hasConnection;
    private String connectionFlight;
    private int connectionDeadlineMinutes;
    private boolean reAccommodated;
    private double compensationPaid;
    private Passenger.PassengerStatus status;
    private LocalDateTime createdAt;

    public static PassengerResponseDto from(Passenger p) {
        return PassengerResponseDto.builder()
                .id(p.getId())
                .flightId(p.getFlight() != null ? p.getFlight().getId() : null)
                .flightNumber(p.getFlight() != null ? p.getFlight().getFlightNumber() : null)
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .bookingReference(p.getBookingReference())
                .seatNumber(p.getSeatNumber())
                .ticketClass(p.getTicketClass())
                .hasConnection(p.isHasConnection())
                .connectionFlight(p.getConnectionFlight())
                .connectionDeadlineMinutes(p.getConnectionDeadlineMinutes())
                .reAccommodated(p.isReAccommodated())
                .compensationPaid(p.getCompensationPaid())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
