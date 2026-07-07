package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.client.OptimizerClient;
import rw.ac.dss.dto.optimizer.AircraftSpareDto;
import rw.ac.dss.dto.optimizer.CrewAvailabilityDto;
import rw.ac.dss.dto.optimizer.FeedbackRequestDto;
import rw.ac.dss.dto.optimizer.FeedbackResponseDto;
import rw.ac.dss.dto.optimizer.OptimizeRequestDto;
import rw.ac.dss.dto.optimizer.OptimizationResponseDto;
import rw.ac.dss.dto.optimizer.RecoveryOptionResultDto;
import rw.ac.dss.dto.request.CreateManualOptionRequest;
import rw.ac.dss.dto.response.RecoveryOptionResponseDto;
import rw.ac.dss.dto.response.RecoveryScenarioResponseDto;
import rw.ac.dss.exception.ConflictException;
import rw.ac.dss.exception.NotFoundException;
import rw.ac.dss.model.Aircraft;
import rw.ac.dss.model.Crew;
import rw.ac.dss.model.DelayEvent;
import rw.ac.dss.model.Flight;
import rw.ac.dss.model.Passenger;
import rw.ac.dss.model.RecoveryOption;
import rw.ac.dss.model.RecoveryScenario;
import rw.ac.dss.repository.AircraftRepository;
import rw.ac.dss.repository.CrewRepository;
import rw.ac.dss.repository.PassengerRepository;
import rw.ac.dss.repository.RecoveryOptionRepository;
import rw.ac.dss.repository.RecoveryScenarioRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Orchestrates the delay recovery flow: gathers the current operational state for a
 * delay event, calls the Python MILP/RL optimizer, and persists the ranked recovery
 * options as a RecoveryScenario.
 */
@Service
@RequiredArgsConstructor
public class RecoveryService {

    private final DelayEventService delayEventService;
    private final AircraftRepository aircraftRepository;
    private final CrewRepository crewRepository;
    private final PassengerRepository passengerRepository;
    private final RecoveryScenarioRepository recoveryScenarioRepository;
    private final RecoveryOptionRepository recoveryOptionRepository;
    private final OptimizerClient optimizerClient;

    @Value("${app.recovery.fuel-cost-per-hour:4500.0}")
    private double defaultFuelCostPerHour;

    @Value("${app.recovery.slot-penalty-per-hour:800.0}")
    private double defaultSlotPenaltyPerHour;

    @Value("${app.recovery.passenger-compensation-rate:150.0}")
    private double defaultPassengerCompensationRate;

    @Transactional
    public RecoveryScenarioResponseDto generateOptions(Long delayEventId) {
        DelayEvent delayEvent = delayEventService.getById(delayEventId);
        Flight flight = delayEvent.getFlight();
        Aircraft delayedAircraft = flight.getAircraft();

        OptimizeRequestDto request = buildOptimizeRequest(delayEvent, flight, delayedAircraft);
        OptimizationResponseDto response = optimizerClient.optimize(request);

        RecoveryScenario scenario = recoveryScenarioRepository.findByDelayEventId(delayEventId)
                .orElseGet(() -> RecoveryScenario.builder()
                        .delayEvent(delayEvent)
                        .scenarioTitle("Recovery for " + flight.getFlightNumber() + " (" + delayEvent.getDelayMinutes() + " min delay)")
                        .build());

        scenario.setStatus(RecoveryScenario.ScenarioStatus.IN_PROGRESS);
        scenario.setPriority(derivePriority(delayEvent));
        scenario.setOptimisationRunAt(LocalDateTime.now());
        scenario.setLastComputationTimeMs(response.getComputationTimeMs());
        RecoveryScenario savedScenario = recoveryScenarioRepository.save(scenario);

        List<RecoveryOption> existing = recoveryOptionRepository.findByScenarioIdOrderByRankAsc(savedScenario.getId());
        if (!existing.isEmpty()) {
            recoveryOptionRepository.deleteAll(existing);
        }

        List<RecoveryOption> persisted = response.getOptions().stream()
                .map(dto -> toRecoveryOption(dto, savedScenario))
                .map(recoveryOptionRepository::save)
                .toList();

        List<RecoveryOptionResponseDto> optionDtos = persisted.stream()
                .map(RecoveryOptionResponseDto::from)
                .collect(Collectors.toList());

        return RecoveryScenarioResponseDto.from(savedScenario, optionDtos, response.getComputationTimeMs());
    }

    @Transactional
    public RecoveryScenarioResponseDto selectOption(Long scenarioId, Long optionId, String decisionMadeBy) {
        RecoveryScenario scenario = recoveryScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Recovery scenario not found: " + scenarioId));

        List<RecoveryOption> options = recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenarioId);
        RecoveryOption chosen = options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Recovery option " + optionId + " does not belong to scenario " + scenarioId));

        for (RecoveryOption option : options) {
            option.setSelected(option.getId().equals(optionId));
        }
        recoveryOptionRepository.saveAll(options);

        scenario.setSelectedOptionId(chosen.getId());
        scenario.setStatus(RecoveryScenario.ScenarioStatus.RESOLVED);
        scenario.setResolvedAt(LocalDateTime.now());
        scenario.setDecisionMadeBy(decisionMadeBy);
        RecoveryScenario saved = recoveryScenarioRepository.save(scenario);

        List<RecoveryOptionResponseDto> optionDtos = options.stream()
                .map(RecoveryOptionResponseDto::from)
                .collect(Collectors.toList());

        return RecoveryScenarioResponseDto.from(saved, optionDtos, saved.getLastComputationTimeMs());
    }

    /**
     * The proposal's "structured override mechanism ... to incorporate tacit
     * operational knowledge not captured in the model" (Section 2.2.4). Adds a
     * controller-authored option alongside the MILP/RL ones and re-ranks everything
     * by cost so it competes fairly with the model's own suggestions.
     */
    @Transactional
    public RecoveryScenarioResponseDto addManualOption(Long scenarioId, CreateManualOptionRequest request) {
        RecoveryScenario scenario = recoveryScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Recovery scenario not found: " + scenarioId));

        if (scenario.getStatus() == RecoveryScenario.ScenarioStatus.RESOLVED) {
            throw new ConflictException(
                    "This scenario is already resolved. Re-run optimization to reopen it before adding another option.");
        }

        String addedBy = request.getAddedBy();
        String notePrefix = "Manually entered override" + (addedBy != null && !addedBy.isBlank() ? " by " + addedBy : "") + ". ";
        RecoveryOption manualOption = RecoveryOption.builder()
                .scenario(scenario)
                .optionType(request.getOptionType())
                .description(request.getDescription())
                .totalCost(request.getTotalCost())
                .crewOvertimeCost(request.getCrewOvertimeCost())
                .fuelCost(request.getFuelCost())
                .passengerCompensationCost(request.getPassengerCompensationCost())
                .slotPenaltyCost(request.getSlotPenaltyCost())
                .mroCost(request.getMroCost())
                .estimatedDelayReduction(request.getEstimatedDelayReduction())
                .feasible(true)
                .feasibilityNotes(notePrefix + (request.getFeasibilityNotes() != null ? request.getFeasibilityNotes() : ""))
                .isSelected(false)
                .generatedBy(RecoveryOption.GeneratedBy.MANUAL)
                .build();
        recoveryOptionRepository.save(manualOption);

        List<RecoveryOption> allOptions = recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenarioId);
        List<RecoveryOptionResponseDto> optionDtos = reRankAndSave(allOptions);

        return RecoveryScenarioResponseDto.from(scenario, optionDtos, scenario.getLastComputationTimeMs());
    }

    private List<RecoveryOptionResponseDto> reRankAndSave(List<RecoveryOption> options) {
        List<RecoveryOption> resorted = options.stream()
                .sorted(java.util.Comparator
                        .comparing(RecoveryOption::isFeasible, java.util.Comparator.reverseOrder())
                        .thenComparingDouble(RecoveryOption::getTotalCost))
                .collect(Collectors.toList());
        for (int i = 0; i < resorted.size(); i++) {
            resorted.get(i).setRank(i + 1);
        }
        recoveryOptionRepository.saveAll(resorted);
        return resorted.stream().map(RecoveryOptionResponseDto::from).collect(Collectors.toList());
    }

    /**
     * The proposal's "Feedback and Learning Loop" (Section 2.2.4): records what a
     * recovery decision actually cost once implemented, computes the realised
     * savings, and feeds the outcome back to the RL agent so it learns from real
     * experience instead of staying frozen at its domain-seeded initial policy.
     */
    @Transactional
    public RecoveryScenarioResponseDto recordOutcome(Long scenarioId, double actualCost, Double manualBaselineCost) {
        RecoveryScenario scenario = recoveryScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Recovery scenario not found: " + scenarioId));

        if (scenario.getStatus() != RecoveryScenario.ScenarioStatus.RESOLVED || scenario.getSelectedOptionId() == null) {
            throw new ConflictException("Select a recovery option before recording its outcome.");
        }

        List<RecoveryOption> options = recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenarioId);
        RecoveryOption selected = options.stream()
                .filter(o -> o.getId().equals(scenario.getSelectedOptionId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Selected option not found for scenario: " + scenarioId));

        double actualCostSaved = selected.getTotalCost() - actualCost;
        scenario.setActualCost(actualCost);
        scenario.setActualCostSaved(actualCostSaved);
        scenario.setOutcomeRecorded(true);
        if (manualBaselineCost != null) {
            scenario.setManualBaselineCost(manualBaselineCost);
        }
        RecoveryScenario saved = recoveryScenarioRepository.save(scenario);

        DelayEvent delayEvent = saved.getDelayEvent();
        Flight flight = delayEvent.getFlight();
        Aircraft delayedAircraft = flight.getAircraft();
        OptimizeRequestDto optimizeRequest = buildOptimizeRequest(delayEvent, flight, delayedAircraft);

        FeedbackResponseDto feedback = optimizerClient.sendFeedback(FeedbackRequestDto.builder()
                .optimisationRequest(optimizeRequest)
                .chosenAction(selected.getOptionType().name())
                .estimatedCost(selected.getTotalCost())
                .actualCost(actualCost)
                .build());

        List<RecoveryOptionResponseDto> optionDtos = options.stream()
                .map(RecoveryOptionResponseDto::from)
                .collect(Collectors.toList());

        RecoveryScenarioResponseDto response = RecoveryScenarioResponseDto.from(saved, optionDtos, saved.getLastComputationTimeMs());
        response.setRlPolicyUpdated(feedback.isUpdated());
        response.setRlFeedbackMessage(feedback.getMessage());
        return response;
    }

    /**
     * Backs the Recovery History page - the proposal's "searchable record of
     * recovery decisions and their outcomes" for post-operational analysis and
     * controller training (Section 2.2.4).
     */
    public List<rw.ac.dss.dto.response.RecoveryScenarioSummaryDto> listScenarios() {
        return recoveryScenarioRepository.findAll().stream()
                .map(scenario -> {
                    RecoveryOption selected = scenario.getSelectedOptionId() == null
                            ? null
                            : recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenario.getId()).stream()
                                    .filter(o -> o.getId().equals(scenario.getSelectedOptionId()))
                                    .findFirst()
                                    .orElse(null);
                    return rw.ac.dss.dto.response.RecoveryScenarioSummaryDto.from(scenario, selected);
                })
                .collect(Collectors.toList());
    }

    public RecoveryScenarioResponseDto getScenario(Long scenarioId) {
        RecoveryScenario scenario = recoveryScenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new NotFoundException("Recovery scenario not found: " + scenarioId));
        List<RecoveryOptionResponseDto> optionDtos = recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenarioId).stream()
                .map(RecoveryOptionResponseDto::from)
                .collect(Collectors.toList());
        return RecoveryScenarioResponseDto.from(scenario, optionDtos, scenario.getLastComputationTimeMs());
    }

    /**
     * Read-only lookup used by the dashboard when revisiting a delay event, so
     * simply viewing a page never re-triggers generateOptions() (which deletes and
     * replaces options - fine for an explicit re-run, but would silently wipe an
     * already-RESOLVED scenario's selection if it fired on every page load).
     */
    public Optional<RecoveryScenarioResponseDto> findScenarioForDelayEvent(Long delayEventId) {
        return recoveryScenarioRepository.findByDelayEventId(delayEventId)
                .map(scenario -> {
                    List<RecoveryOptionResponseDto> optionDtos = recoveryOptionRepository
                            .findByScenarioIdOrderByRankAsc(scenario.getId()).stream()
                            .map(RecoveryOptionResponseDto::from)
                            .collect(Collectors.toList());
                    return RecoveryScenarioResponseDto.from(scenario, optionDtos, scenario.getLastComputationTimeMs());
                });
    }

    private OptimizeRequestDto buildOptimizeRequest(DelayEvent delayEvent, Flight flight, Aircraft delayedAircraft) {
        List<Passenger> passengers = passengerRepository.findByFlightId(flight.getId());
        int connectionPassengers = (int) passengers.stream().filter(Passenger::isHasConnection).count();
        int passengerCount = flight.getPassengerCount() > 0 ? flight.getPassengerCount() : passengers.size();

        List<AircraftSpareDto> spares = aircraftRepository
                .findByStatusAndIdNot(Aircraft.AircraftStatus.SERVICEABLE, delayedAircraft.getId())
                .stream()
                .map(this::toAircraftSpareDto)
                .collect(Collectors.toList());

        List<CrewAvailabilityDto> crew = crewRepository.findByStatus(Crew.CrewStatus.AVAILABLE)
                .stream()
                .map(this::toCrewAvailabilityDto)
                .collect(Collectors.toList());

        return OptimizeRequestDto.builder()
                .delayEventId(delayEvent.getId())
                .aircraftId(delayedAircraft.getId())
                .flightNumber(flight.getFlightNumber())
                .origin(flight.getOrigin())
                .destination(flight.getDestination())
                .delayMinutes(delayEvent.getDelayMinutes())
                .delayCategory(delayEvent.getDelayCategory().name())
                .passengerCount(passengerCount)
                .connectionPassengers(connectionPassengers)
                .availableAircraft(spares)
                .availableCrew(crew)
                .fuelCostPerHour(defaultFuelCostPerHour)
                .slotPenaltyPerHour(defaultSlotPenaltyPerHour)
                .passengerCompensationRate(defaultPassengerCompensationRate)
                .aircraftType(shortTypeCode(delayedAircraft.getAircraftType()))
                .build();
    }

    private AircraftSpareDto toAircraftSpareDto(Aircraft aircraft) {
        double hoursSinceMaintenance = aircraft.getLastMaintenanceDate() != null
                ? ChronoUnit.HOURS.between(aircraft.getLastMaintenanceDate().atStartOfDay(), LocalDateTime.now())
                : 999.0;
        return AircraftSpareDto.builder()
                .id(aircraft.getId())
                .type(shortTypeCode(aircraft.getAircraftType()))
                .location(aircraft.getCurrentLocation())
                .seats(aircraft.getTotalSeats())
                .status(aircraft.getStatus().name())
                .lastMaintenanceHours(hoursSinceMaintenance)
                .build();
    }

    private CrewAvailabilityDto toCrewAvailabilityDto(Crew crew) {
        return CrewAvailabilityDto.builder()
                .id(crew.getId())
                .role(crew.getRole().name())
                .qualification(crew.getQualifications())
                .dutyHoursRemaining(Math.max(0.0, crew.getMaxDutyHours() - crew.getDutyHoursUsed()))
                .overtimeRate(crew.getOvertimeRate())
                .build();
    }

    private String shortTypeCode(Aircraft.AircraftType type) {
        return switch (type) {
            case BOEING_737 -> "B737";
            case AIRBUS_A330 -> "A330";
            case TURBOPROP_Q400 -> "Q400";
        };
    }

    private RecoveryScenario.ScenarioPriority derivePriority(DelayEvent delayEvent) {
        if (delayEvent.getDelayCategory() == DelayEvent.DelayCategory.TECHNICAL && delayEvent.getDelayMinutes() > 180) {
            return RecoveryScenario.ScenarioPriority.CRITICAL;
        }
        if (delayEvent.getDelayMinutes() > 120) {
            return RecoveryScenario.ScenarioPriority.HIGH;
        }
        if (delayEvent.getDelayMinutes() > 30) {
            return RecoveryScenario.ScenarioPriority.MEDIUM;
        }
        return RecoveryScenario.ScenarioPriority.LOW;
    }

    private RecoveryOption toRecoveryOption(RecoveryOptionResultDto dto, RecoveryScenario scenario) {
        return RecoveryOption.builder()
                .scenario(scenario)
                .optionType(RecoveryOption.OptionType.valueOf(dto.getOptionType()))
                .description(dto.getDescription())
                .rank(dto.getRank())
                .totalCost(dto.getTotalCost())
                .crewOvertimeCost(dto.getCrewOvertimeCost())
                .fuelCost(dto.getFuelCost())
                .passengerCompensationCost(dto.getPassengerCompensationCost())
                .slotPenaltyCost(dto.getSlotPenaltyCost())
                .mroCost(dto.getMroCost())
                .estimatedDelayReduction(dto.getEstimatedDelayReduction())
                .feasible(dto.isFeasible())
                .feasibilityNotes(dto.getFeasibilityNotes())
                .isSelected(false)
                .generatedBy(RecoveryOption.GeneratedBy.valueOf(dto.getGeneratedBy()))
                .passengerImpactScore(dto.getPassengerImpactScore())
                .crewDutyCompliance(dto.getCrewDutyCompliance() != null
                        ? RecoveryOption.CrewDutyCompliance.valueOf(dto.getCrewDutyCompliance())
                        : RecoveryOption.CrewDutyCompliance.UNKNOWN)
                .regulatoryFeasible(dto.isRegulatoryFeasible())
                .build();
    }
}
