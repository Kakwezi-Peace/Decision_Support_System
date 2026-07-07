package rw.ac.dss.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rw.ac.dss.model.Crew;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {

    List<Crew> findByStatus(Crew.CrewStatus status);
}
