package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.RecoveryScenario;

import java.util.Optional;

public interface RecoveryScenarioRepository extends JpaRepository<RecoveryScenario, Long> {

    Optional<RecoveryScenario> findByDelayEventId(Long delayEventId);
}
