package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.Flight;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    boolean existsByAircraftId(Long aircraftId);
}
