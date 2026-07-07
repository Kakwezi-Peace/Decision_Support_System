package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.CreateCrewRequest;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.Crew;
import rw.ac.dss.repository.CrewRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrewService {

    private final CrewRepository crewRepository;

    @Transactional
    public Crew create(CreateCrewRequest request) {
        Crew crew = Crew.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .employeeId(request.getEmployeeId())
                .role(request.getRole())
                .qualifications(request.getQualifications())
                .dutyHoursUsed(request.getDutyHoursUsed())
                .maxDutyHours(request.getMaxDutyHours() != null ? request.getMaxDutyHours() : 14.0)
                .currentLocation(request.getCurrentLocation())
                .status(request.getStatus())
                .overtimeRate(request.getOvertimeRate())
                .build();
        return crewRepository.save(crew);
    }

    public Crew getById(Long id) {
        return crewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Crew member not found: " + id));
    }

    public List<Crew> list() {
        return crewRepository.findAll();
    }

    @Transactional
    public Crew update(Long id, CreateCrewRequest request) {
        Crew crew = getById(id);
        crew.setFirstName(request.getFirstName());
        crew.setLastName(request.getLastName());
        crew.setEmployeeId(request.getEmployeeId());
        crew.setRole(request.getRole());
        crew.setQualifications(request.getQualifications());
        crew.setDutyHoursUsed(request.getDutyHoursUsed());
        crew.setMaxDutyHours(request.getMaxDutyHours() != null ? request.getMaxDutyHours() : 14.0);
        crew.setCurrentLocation(request.getCurrentLocation());
        crew.setStatus(request.getStatus());
        crew.setOvertimeRate(request.getOvertimeRate());
        return crewRepository.save(crew);
    }

    @Transactional
    public void delete(Long id) {
        Crew crew = getById(id);
        crewRepository.delete(crew);
    }
}
