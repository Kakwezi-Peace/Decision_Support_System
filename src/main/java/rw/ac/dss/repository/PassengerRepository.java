package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.Passenger;

import java.util.List;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    List<Passenger> findByFlightId(Long flightId);
}
