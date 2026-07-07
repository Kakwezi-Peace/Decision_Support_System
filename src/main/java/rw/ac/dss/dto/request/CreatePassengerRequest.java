package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rw.ac.dss.model.Passenger;

@Data
public class CreatePassengerRequest {

    @NotNull
    private Long flightId;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String bookingReference;

    private String seatNumber;

    @NotNull
    private Passenger.TicketClass ticketClass;

    private boolean hasConnection;

    private String connectionFlight;

    private int connectionDeadlineMinutes;

    private boolean reAccommodated;

    private double compensationPaid;

    @NotNull
    private Passenger.PassengerStatus status;
}
