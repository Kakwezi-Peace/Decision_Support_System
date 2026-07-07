package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.request.CreateDelayEventRequest;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.DelayEvent;
import rw.ac.dss.model.Flight;
import rw.ac.dss.repository.DelayEventRepository;
import rw.ac.dss.repository.RecoveryOptionRepository;
import rw.ac.dss.repository.RecoveryScenarioRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DelayEventService {

    private final DelayEventRepository delayEventRepository;
    private final FlightService flightService;
    // Injected directly (not via RecoveryService) to avoid a circular bean
    // dependency - RecoveryService already depends on DelayEventService.
    private final RecoveryScenarioRepository recoveryScenarioRepository;
    private final RecoveryOptionRepository recoveryOptionRepository;

    @Transactional
    public DelayEvent create(CreateDelayEventRequest request) {
        Flight flight = flightService.getById(request.getFlightId());

        DelayEvent delayEvent = DelayEvent.builder()
                .flight(flight)
                .delayCode(request.getDelayCode())
                .delayCause(request.getDelayCause())
                .delayCategory(request.getDelayCategory())
                .delayMinutes(request.getDelayMinutes())
                .estimatedDirectCost(request.getEstimatedDirectCost())
                .estimatedPassengerCost(request.getEstimatedPassengerCost())
                .totalEstimatedCost(request.getTotalEstimatedCost())
                .reportedAt(LocalDateTime.now())
                .reportedBy(request.getReportedBy())
                .status(DelayEvent.DelayStatus.ACTIVE)
                .notes(request.getNotes())
                .build();
        return delayEventRepository.save(delayEvent);
    }

    public DelayEvent getById(Long id) {
        return delayEventRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Delay event not found: " + id));
    }

    public List<DelayEvent> list() {
        return delayEventRepository.findAll();
    }

    @Transactional
    public DelayEvent update(Long id, CreateDelayEventRequest request) {
        DelayEvent delayEvent = getById(id);
        Flight flight = flightService.getById(request.getFlightId());

        delayEvent.setFlight(flight);
        delayEvent.setDelayCode(request.getDelayCode());
        delayEvent.setDelayCause(request.getDelayCause());
        delayEvent.setDelayCategory(request.getDelayCategory());
        delayEvent.setDelayMinutes(request.getDelayMinutes());
        delayEvent.setEstimatedDirectCost(request.getEstimatedDirectCost());
        delayEvent.setEstimatedPassengerCost(request.getEstimatedPassengerCost());
        delayEvent.setTotalEstimatedCost(request.getTotalEstimatedCost());
        delayEvent.setReportedBy(request.getReportedBy());
        delayEvent.setNotes(request.getNotes());
        return delayEventRepository.save(delayEvent);
    }

    /**
     * Deleting a delay event also removes its recovery scenario/options - those are
     * generated artefacts of this delay event, not independent records worth keeping
     * once the delay event itself is gone.
     */
    @Transactional
    public void delete(Long id) {
        DelayEvent delayEvent = getById(id);
        recoveryScenarioRepository.findByDelayEventId(id).ifPresent(scenario -> {
            recoveryOptionRepository.deleteAll(recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenario.getId()));
            recoveryScenarioRepository.delete(scenario);
        });
        delayEventRepository.delete(delayEvent);
    }
}
