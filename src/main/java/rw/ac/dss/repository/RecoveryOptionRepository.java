package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.RecoveryOption;

import java.util.List;

public interface RecoveryOptionRepository extends JpaRepository<RecoveryOption, Long> {

    List<RecoveryOption> findByScenarioIdOrderByRankAsc(Long scenarioId);
}
