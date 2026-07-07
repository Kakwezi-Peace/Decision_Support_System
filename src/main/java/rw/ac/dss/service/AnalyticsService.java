package rw.ac.dss.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.dss.dto.response.AnalyticsSummaryDto;
import rw.ac.dss.model.DelayEvent;
import rw.ac.dss.model.RecoveryOption;
import rw.ac.dss.model.RecoveryScenario;
import rw.ac.dss.repository.DelayEventRepository;
import rw.ac.dss.repository.RecoveryOptionRepository;
import rw.ac.dss.repository.RecoveryScenarioRepository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final DelayEventRepository delayEventRepository;
    private final RecoveryScenarioRepository recoveryScenarioRepository;
    private final RecoveryOptionRepository recoveryOptionRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummaryDto getSummary() {
        List<DelayEvent> delayEvents = delayEventRepository.findAll();
        List<RecoveryScenario> scenarios = recoveryScenarioRepository.findAll();

        Map<String, Long> countByCategory = delayEvents.stream()
                .collect(Collectors.groupingBy(d -> d.getDelayCategory().name(), Collectors.counting()));

        Map<String, Double> avgMinutesByCategory = delayEvents.stream()
                .collect(Collectors.groupingBy(d -> d.getDelayCategory().name(), Collectors.averagingInt(DelayEvent::getDelayMinutes)));

        Map<String, Double> costByCategory = scenarios.stream()
                .filter(RecoveryScenario::isOutcomeRecorded)
                .collect(Collectors.groupingBy(
                        s -> s.getDelayEvent().getDelayCategory().name(),
                        Collectors.summingDouble(RecoveryScenario::getActualCost)));

        long resolved = scenarios.stream().filter(s -> s.getStatus() == RecoveryScenario.ScenarioStatus.RESOLVED).count();
        long pending = scenarios.stream().filter(s -> s.getStatus() == RecoveryScenario.ScenarioStatus.IN_PROGRESS).count();

        double totalCostSaved = scenarios.stream()
                .filter(RecoveryScenario::isOutcomeRecorded)
                .mapToDouble(RecoveryScenario::getActualCostSaved)
                .sum();
        double totalActualCost = scenarios.stream()
                .filter(RecoveryScenario::isOutcomeRecorded)
                .mapToDouble(RecoveryScenario::getActualCost)
                .sum();

        double totalEstimatedCostOfSelected = scenarios.stream()
                .filter(s -> s.getSelectedOptionId() != null)
                .mapToDouble(s -> selectedOptionCost(s))
                .sum();

        OptionalDouble avgComputation = scenarios.stream()
                .filter(s -> s.getLastComputationTimeMs() != null && s.getLastComputationTimeMs() > 0)
                .mapToDouble(RecoveryScenario::getLastComputationTimeMs)
                .average();

        OptionalDouble avgDecisionMinutes = scenarios.stream()
                .filter(s -> s.getOptimisationRunAt() != null && s.getResolvedAt() != null)
                .mapToDouble(s -> Duration.between(s.getOptimisationRunAt(), s.getResolvedAt()).toSeconds() / 60.0)
                .average();

        List<RecoveryScenario> withBaseline = scenarios.stream()
                .filter(s -> s.getManualBaselineCost() != null && s.isOutcomeRecorded())
                .toList();
        Double manualBaselineTotal = withBaseline.isEmpty()
                ? null
                : round2(withBaseline.stream().mapToDouble(RecoveryScenario::getManualBaselineCost).sum());
        Double actualCostForBaselineComparison = withBaseline.isEmpty()
                ? null
                : round2(withBaseline.stream().mapToDouble(RecoveryScenario::getActualCost).sum());
        Double savingsPercent = null;
        if (manualBaselineTotal != null && manualBaselineTotal > 0) {
            savingsPercent = round2(((manualBaselineTotal - actualCostForBaselineComparison) / manualBaselineTotal) * 100.0);
        }

        return AnalyticsSummaryDto.builder()
                .totalDelayEvents(delayEvents.size())
                .totalResolvedScenarios(resolved)
                .totalPendingScenarios(pending)
                .delayCountByCategory(countByCategory)
                .avgDelayMinutesByCategory(avgMinutesByCategory)
                .costByCategory(costByCategory)
                .totalCostSaved(round2(totalCostSaved))
                .totalEstimatedCostOfSelectedOptions(round2(totalEstimatedCostOfSelected))
                .totalActualCost(round2(totalActualCost))
                .avgComputationTimeMs(avgComputation.isPresent() ? round2(avgComputation.getAsDouble()) : null)
                .avgDecisionMinutes(avgDecisionMinutes.isPresent() ? round2(avgDecisionMinutes.getAsDouble()) : null)
                .manualBaselineTotal(manualBaselineTotal)
                .actualCostForBaselineComparison(actualCostForBaselineComparison)
                .manualVsDssSavingsPercent(savingsPercent)
                .scenariosWithManualBaseline(withBaseline.size())
                .build();
    }

    private double selectedOptionCost(RecoveryScenario scenario) {
        return recoveryOptionRepository.findByScenarioIdOrderByRankAsc(scenario.getId()).stream()
                .filter(o -> o.getId().equals(scenario.getSelectedOptionId()))
                .findFirst()
                .map(RecoveryOption::getTotalCost)
                .orElse(0.0);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
