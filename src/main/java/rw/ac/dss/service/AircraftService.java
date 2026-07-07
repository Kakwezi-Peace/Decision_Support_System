package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.CreateAircraftRequest;
import rw.ac.dss.exception.ConflictException;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.Aircraft;
import rw.ac.dss.repository.AircraftRepository;
import rw.ac.dss.repository.FlightRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AircraftService {

    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;

    @Transactional
    public Aircraft create(CreateAircraftRequest request) {
        Aircraft aircraft = Aircraft.builder()
                .registrationNumber(request.getRegistrationNumber())
                .aircraftType(request.getAircraftType())
                .status(request.getStatus())
                .currentLocation(request.getCurrentLocation())
                .totalSeats(request.getTotalSeats())
                .yearManufactured(request.getYearManufactured())
                .lastMaintenanceDate(request.getLastMaintenanceDate())
                .notes(request.getNotes())
                .build();
        return aircraftRepository.save(aircraft);
    }

    public Aircraft getById(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Aircraft not found: " + id));
    }

    public List<Aircraft> list() {
        return aircraftRepository.findAll();
    }

    @Transactional
    public Aircraft update(Long id, CreateAircraftRequest request) {
        Aircraft aircraft = getById(id);
        aircraft.setRegistrationNumber(request.getRegistrationNumber());
        aircraft.setAircraftType(request.getAircraftType());
        aircraft.setStatus(request.getStatus());
        aircraft.setCurrentLocation(request.getCurrentLocation());
        aircraft.setTotalSeats(request.getTotalSeats());
        aircraft.setYearManufactured(request.getYearManufactured());
        aircraft.setLastMaintenanceDate(request.getLastMaintenanceDate());
        aircraft.setNotes(request.getNotes());
        return aircraftRepository.save(aircraft);
    }

    @Transactional
    public void delete(Long id) {
        Aircraft aircraft = getById(id);
        if (flightRepository.existsByAircraftId(id)) {
            throw new ConflictException(
                    "Cannot delete aircraft " + aircraft.getRegistrationNumber() + ": it is assigned to one or more flights.");
        }
        aircraftRepository.delete(aircraft);
    }
}
