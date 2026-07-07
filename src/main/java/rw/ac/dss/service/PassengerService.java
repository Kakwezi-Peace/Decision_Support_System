package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.CreatePassengerRequest;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.Flight;
import rw.ac.dss.model.Passenger;
import rw.ac.dss.repository.PassengerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final FlightService flightService;

    @Transactional
    public Passenger create(CreatePassengerRequest request) {
        Flight flight = flightService.getById(request.getFlightId());

        Passenger passenger = Passenger.builder()
                .flight(flight)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .bookingReference(request.getBookingReference())
                .seatNumber(request.getSeatNumber())
                .ticketClass(request.getTicketClass())
                .hasConnection(request.isHasConnection())
                .connectionFlight(request.getConnectionFlight())
                .connectionDeadlineMinutes(request.getConnectionDeadlineMinutes())
                .reAccommodated(request.isReAccommodated())
                .compensationPaid(request.getCompensationPaid())
                .status(request.getStatus())
                .build();
        return passengerRepository.save(passenger);
    }

    public Passenger getById(Long id) {
        return passengerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Passenger not found: " + id));
    }

    public List<Passenger> list() {
        return passengerRepository.findAll();
    }

    @Transactional
    public Passenger update(Long id, CreatePassengerRequest request) {
        Passenger passenger = getById(id);
        Flight flight = flightService.getById(request.getFlightId());

        passenger.setFlight(flight);
        passenger.setFirstName(request.getFirstName());
        passenger.setLastName(request.getLastName());
        passenger.setBookingReference(request.getBookingReference());
        passenger.setSeatNumber(request.getSeatNumber());
        passenger.setTicketClass(request.getTicketClass());
        passenger.setHasConnection(request.isHasConnection());
        passenger.setConnectionFlight(request.getConnectionFlight());
        passenger.setConnectionDeadlineMinutes(request.getConnectionDeadlineMinutes());
        passenger.setReAccommodated(request.isReAccommodated());
        passenger.setCompensationPaid(request.getCompensationPaid());
        passenger.setStatus(request.getStatus());
        return passengerRepository.save(passenger);
    }

    @Transactional
    public void delete(Long id) {
        Passenger passenger = getById(id);
        passengerRepository.delete(passenger);
    }
}
