package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.DelayEvent;

public interface DelayEventRepository extends JpaRepository<DelayEvent, Long> {

    boolean existsByFlightId(Long flightId);
}
