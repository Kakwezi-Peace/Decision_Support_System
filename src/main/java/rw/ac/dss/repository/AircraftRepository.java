package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.Aircraft;

import java.util.List;

public interface AircraftRepository extends JpaRepository<Aircraft, Long> {

    List<Aircraft> findByStatusAndIdNot(Aircraft.AircraftStatus status, Long id);
}
