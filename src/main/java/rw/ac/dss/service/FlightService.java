package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.CreateFlightRequest;
import rw.ac.dss.exception.ConflictException;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.Aircraft;
import rw.ac.dss.model.Flight;
import rw.ac.dss.repository.DelayEventRepository;
import rw.ac.dss.repository.FlightRepository;
import rw.ac.dss.repository.PassengerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;
    private final AircraftService aircraftService;
    private final DelayEventRepository delayEventRepository;
    private final PassengerRepository passengerRepository;

    @Transactional
    public Flight create(CreateFlightRequest request) {
        Aircraft aircraft = aircraftService.getById(request.getAircraftId());

        Flight flight = Flight.builder()
                .flightNumber(request.getFlightNumber())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .scheduledDeparture(request.getScheduledDeparture())
                .scheduledArrival(request.getScheduledArrival())
                .status(request.getStatus() != null ? request.getStatus() : Flight.FlightStatus.SCHEDULED)
                .aircraft(aircraft)
                .passengerCount(request.getPassengerCount())
                .availableSeats(request.getAvailableSeats())
                .fuelCost(request.getFuelCost())
                .notes(request.getNotes())
                .build();
        return flightRepository.save(flight);
    }

    public Flight getById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Flight not found: " + id));
    }

    public List<Flight> list() {
        return flightRepository.findAll();
    }

    @Transactional
    public Flight update(Long id, CreateFlightRequest request) {
        Flight flight = getById(id);
        Aircraft aircraft = aircraftService.getById(request.getAircraftId());

        flight.setFlightNumber(request.getFlightNumber());
        flight.setOrigin(request.getOrigin());
        flight.setDestination(request.getDestination());
        flight.setScheduledDeparture(request.getScheduledDeparture());
        flight.setScheduledArrival(request.getScheduledArrival());
        flight.setStatus(request.getStatus() != null ? request.getStatus() : flight.getStatus());
        flight.setAircraft(aircraft);
        flight.setPassengerCount(request.getPassengerCount());
        flight.setAvailableSeats(request.getAvailableSeats());
        flight.setFuelCost(request.getFuelCost());
        flight.setNotes(request.getNotes());
        return flightRepository.save(flight);
    }

    @Transactional
    public void delete(Long id) {
        Flight flight = getById(id);
        if (delayEventRepository.existsByFlightId(id)) {
            throw new ConflictException(
                    "Cannot delete flight " + flight.getFlightNumber() + ": it has one or more delay events recorded against it.");
        }
        if (!passengerRepository.findByFlightId(id).isEmpty()) {
            throw new ConflictException(
                    "Cannot delete flight " + flight.getFlightNumber() + ": it has passengers booked on it.");
        }
        flightRepository.delete(flight);
    }
}
