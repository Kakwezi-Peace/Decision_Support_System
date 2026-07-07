package rw.ac.dss.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import rw.ac.dss.client.OptimizerClient;
import rw.ac.dss.dto.optimizer.OptimizationResponseDto;
import rw.ac.dss.dto.optimizer.OptimizeRequestDto;
import rw.ac.dss.dto.optimizer.RecoveryOptionResultDto;
import rw.ac.dss.dto.response.RecoveryScenarioResponseDto;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RecoveryServiceTest {

    @Mock
    private DelayEventService delayEventService;
    @Mock
    private AircraftRepository aircraftRepository;
    @Mock
    private CrewRepository crewRepository;
    @Mock
    private PassengerRepository passengerRepository;
    @Mock
    private RecoveryScenarioRepository recoveryScenarioRepository;
    @Mock
    private RecoveryOptionRepository recoveryOptionRepository;
    @Mock
    private OptimizerClient optimizerClient;

    @InjectMocks
    private RecoveryService recoveryService;

    private Aircraft delayedAircraft;
    private Aircraft spareAircraft;
    private Flight flight;
    private DelayEvent delayEvent;
    private final AtomicLong idSequence = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recoveryService, "defaultFuelCostPerHour", 4500.0);
        ReflectionTestUtils.setField(recoveryService, "defaultSlotPenaltyPerHour", 800.0);
        ReflectionTestUtils.setField(recoveryService, "defaultPassengerCompensationRate", 150.0);

        delayedAircraft = Aircraft.builder()
                .id(1L)
                .registrationNumber("9XR-WA1")
                .aircraftType(Aircraft.AircraftType.BOEING_737)
                .status(Aircraft.AircraftStatus.UNSERVICEABLE)
                .currentLocation("KGL")
                .totalSeats(150)
                .build();

        spareAircraft = Aircraft.builder()
                .id(2L)
                .registrationNumber("9XR-WA2")
                .aircraftType(Aircraft.AircraftType.BOEING_737)
                .status(Aircraft.AircraftStatus.SERVICEABLE)
                .currentLocation("NBO")
                .totalSeats(150)
                .build();

        flight = Flight.builder()
                .id(1L)
                .flightNumber("WB301")
                .origin("KGL")
                .destination("NBO")
                .aircraft(delayedAircraft)
                .passengerCount(120)
                .build();

        delayEvent = DelayEvent.builder()
                .id(1L)
                .flight(flight)
                .delayCategory(DelayEvent.DelayCategory.TECHNICAL)
                .delayMinutes(150)
                .build();
    }

    private RecoveryOptionResultDto sampleOption(String type, double cost, int rank, String generatedBy) {
        return RecoveryOptionResultDto.builder()
                .optionType(type)
                .description("desc " + type)
                .rank(rank)
                .totalCost(cost)
                .crewOvertimeCost(0.0)
                .fuelCost(0.0)
                .passengerCompensationCost(0.0)
                .slotPenaltyCost(0.0)
                .mroCost(0.0)
                .estimatedDelayReduction(0)
                .feasible(true)
                .feasibilityNotes("ok")
                .generatedBy(generatedBy)
                .build();
    }

    private void stubHappyPathRepositories() {
        when(delayEventService.getById(1L)).thenReturn(delayEvent);
        when(passengerRepository.findByFlightId(1L)).thenReturn(List.of(
                Passenger.builder().id(1L).flight(flight).hasConnection(true).build(),
                Passenger.builder().id(2L).flight(flight).hasConnection(false).build()
        ));
        when(aircraftRepository.findByStatusAndIdNot(Aircraft.AircraftStatus.SERVICEABLE, 1L))
                .thenReturn(List.of(spareAircraft));
        when(crewRepository.findByStatus(Crew.CrewStatus.AVAILABLE)).thenReturn(List.of(
                Crew.builder().id(1L).role(Crew.CrewRole.CAPTAIN).qualifications("B737,A330")
                        .maxDutyHours(14.0).dutyHoursUsed(2.0).overtimeRate(90.0).build()
        ));
        when(recoveryScenarioRepository.findByDelayEventId(1L)).thenReturn(Optional.empty());
        when(recoveryScenarioRepository.save(any(RecoveryScenario.class))).thenAnswer(inv -> {
            RecoveryScenario s = inv.getArgument(0);
            if (s.getId() == null) {
                s.setId(idSequence.incrementAndGet());
            }
            return s;
        });
        when(recoveryOptionRepository.findByScenarioIdOrderByRankAsc(any())).thenReturn(List.of());
        when(recoveryOptionRepository.save(any(RecoveryOption.class))).thenAnswer(inv -> {
            RecoveryOption o = inv.getArgument(0);
            if (o.getId() == null) {
                o.setId(idSequence.incrementAndGet());
            }
            return o;
        });
    }

    @Test
    void generateOptions_buildsRequestFromCurrentDbState() {
        stubHappyPathRepositories();
        OptimizationResponseDto response = OptimizationResponseDto.builder()
                .delayEventId(1L)
                .options(List.of(
                        sampleOption("CREW_SUBSTITUTION", 400.0, 1, "MILP"),
                        sampleOption("AIRCRAFT_SWAP", 6750.0, 2, "RL")
                ))
                .computationTimeMs(12.5)
                .recommendedOptionIndex(0)
                .build();
        when(optimizerClient.optimize(any(OptimizeRequestDto.class))).thenReturn(response);

        RecoveryScenarioResponseDto result = recoveryService.generateOptions(1L);

        ArgumentCaptor<OptimizeRequestDto> captor = ArgumentCaptor.forClass(OptimizeRequestDto.class);
        verify(optimizerClient).optimize(captor.capture());
        OptimizeRequestDto sentRequest = captor.getValue();

        assertThat(sentRequest.getDelayEventId()).isEqualTo(1L);
        assertThat(sentRequest.getAircraftId()).isEqualTo(1L);
        assertThat(sentRequest.getFlightNumber()).isEqualTo("WB301");
        assertThat(sentRequest.getDelayMinutes()).isEqualTo(150);
        assertThat(sentRequest.getDelayCategory()).isEqualTo("TECHNICAL");
        assertThat(sentRequest.getPassengerCount()).isEqualTo(120);
        assertThat(sentRequest.getConnectionPassengers()).isEqualTo(1);
        assertThat(sentRequest.getAvailableAircraft()).hasSize(1);
        assertThat(sentRequest.getAvailableAircraft().get(0).getId()).isEqualTo(2L);
        assertThat(sentRequest.getAvailableCrew()).hasSize(1);
        assertThat(sentRequest.getAvailableCrew().get(0).getDutyHoursRemaining()).isEqualTo(12.0);
        assertThat(sentRequest.getFuelCostPerHour()).isEqualTo(4500.0);
        assertThat(sentRequest.getAircraftType()).isEqualTo("B737");

        assertThat(result.getStatus()).isEqualTo(RecoveryScenario.ScenarioStatus.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(RecoveryScenario.ScenarioPriority.HIGH);
        assertThat(result.getOptions()).hasSize(2);
        assertThat(result.getOptions().get(0).getOptionType()).isEqualTo(RecoveryOption.OptionType.CREW_SUBSTITUTION);
    }

    @Test
    void generateOptions_reRun_deletesPreviousOptionsBeforePersistingNew() {
        RecoveryScenario existingScenario = RecoveryScenario.builder().id(5L).delayEvent(delayEvent).build();
        List<RecoveryOption> existingOptions = List.of(
                RecoveryOption.builder().id(50L).scenario(existingScenario).build(),
                RecoveryOption.builder().id(51L).scenario(existingScenario).build()
        );

        when(delayEventService.getById(1L)).thenReturn(delayEvent);
        when(passengerRepository.findByFlightId(1L)).thenReturn(List.of());
        when(aircraftRepository.findByStatusAndIdNot(any(), anyLong())).thenReturn(List.of());
        when(crewRepository.findByStatus(any())).thenReturn(List.of());
        when(recoveryScenarioRepository.findByDelayEventId(1L)).thenReturn(Optional.of(existingScenario));
        when(recoveryScenarioRepository.save(any(RecoveryScenario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(recoveryOptionRepository.findByScenarioIdOrderByRankAsc(5L)).thenReturn(existingOptions);
        when(recoveryOptionRepository.save(any(RecoveryOption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(optimizerClient.optimize(any())).thenReturn(OptimizationResponseDto.builder()
                .delayEventId(1L)
                .options(List.of(sampleOption("DELAY_ABSORPTION", 100.0, 1, "MILP")))
                .computationTimeMs(1.0)
                .recommendedOptionIndex(0)
                .build());

        recoveryService.generateOptions(1L);

        verify(recoveryOptionRepository).deleteAll(existingOptions);
    }

    @Test
    void selectOption_marksChosenOptionSelected_andResolvesScenario() {
        RecoveryScenario scenario = RecoveryScenario.builder().id(5L).delayEvent(delayEvent)
                .status(RecoveryScenario.ScenarioStatus.IN_PROGRESS).build();
        RecoveryOption option10 = RecoveryOption.builder().id(10L).scenario(scenario).rank(1).build();
        RecoveryOption option11 = RecoveryOption.builder().id(11L).scenario(scenario).rank(2).build();

        when(recoveryScenarioRepository.findById(5L)).thenReturn(Optional.of(scenario));
        when(recoveryOptionRepository.findByScenarioIdOrderByRankAsc(5L)).thenReturn(List.of(option10, option11));
        when(recoveryOptionRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recoveryScenarioRepository.save(any(RecoveryScenario.class))).thenAnswer(inv -> inv.getArgument(0));

        RecoveryScenarioResponseDto result = recoveryService.selectOption(5L, 11L, "Jean Mugisha");

        assertThat(option10.isSelected()).isFalse();
        assertThat(option11.isSelected()).isTrue();
        assertThat(scenario.getStatus()).isEqualTo(RecoveryScenario.ScenarioStatus.RESOLVED);
        assertThat(scenario.getSelectedOptionId()).isEqualTo(11L);
        assertThat(scenario.getDecisionMadeBy()).isEqualTo("Jean Mugisha");
        assertThat(scenario.getResolvedAt()).isNotNull();
        assertThat(result.getSelectedOptionId()).isEqualTo(11L);
    }

    @Test
    void selectOption_unknownOption_throwsNotFound() {
        RecoveryScenario scenario = RecoveryScenario.builder().id(5L).build();
        when(recoveryScenarioRepository.findById(5L)).thenReturn(Optional.of(scenario));
        when(recoveryOptionRepository.findByScenarioIdOrderByRankAsc(5L)).thenReturn(List.of(
                RecoveryOption.builder().id(10L).scenario(scenario).build()
        ));

        assertThrows(NotFoundException.class, () -> recoveryService.selectOption(5L, 999L, "Someone"));
    }

    @Test
    void selectOption_unknownScenario_throwsNotFound() {
        when(recoveryScenarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> recoveryService.selectOption(999L, 1L, "Someone"));
    }

    @Test
    void generateOptions_lowSeverityDelay_getsLowPriority() {
        delayEvent = DelayEvent.builder().id(1L).flight(flight)
                .delayCategory(DelayEvent.DelayCategory.ATC).delayMinutes(10).build();
        stubHappyPathRepositories();
        when(delayEventService.getById(1L)).thenReturn(delayEvent);
        when(optimizerClient.optimize(any())).thenReturn(OptimizationResponseDto.builder()
                .delayEventId(1L)
                .options(List.of(sampleOption("DELAY_ABSORPTION", 50.0, 1, "MILP")))
                .computationTimeMs(1.0)
                .recommendedOptionIndex(0)
                .build());

        RecoveryScenarioResponseDto result = recoveryService.generateOptions(1L);

        assertThat(result.getPriority()).isEqualTo(RecoveryScenario.ScenarioPriority.LOW);
    }

    @Test
    void generateOptions_technicalLongDelay_getsCriticalPriority() {
        delayEvent = DelayEvent.builder().id(1L).flight(flight)
                .delayCategory(DelayEvent.DelayCategory.TECHNICAL).delayMinutes(200).build();
        stubHappyPathRepositories();
        when(delayEventService.getById(1L)).thenReturn(delayEvent);
        when(optimizerClient.optimize(any())).thenReturn(OptimizationResponseDto.builder()
                .delayEventId(1L)
                .options(List.of(sampleOption("FLIGHT_CANCELLATION", 69000.0, 1, "MILP")))
                .computationTimeMs(1.0)
                .recommendedOptionIndex(0)
                .build());

        RecoveryScenarioResponseDto result = recoveryService.generateOptions(1L);

        assertThat(result.getPriority()).isEqualTo(RecoveryScenario.ScenarioPriority.CRITICAL);
    }
}
