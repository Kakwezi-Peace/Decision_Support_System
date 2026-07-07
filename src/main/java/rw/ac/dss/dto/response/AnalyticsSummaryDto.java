package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Aggregate analytics backing the proposal's evaluation objective: "evaluate the
 * performance of the DSS against RwandAir's current manual decision-making approach
 * in terms of recovery cost efficiency, decision speed, and operational feasibility"
 * (Chapter 1), plus its Chapter 3 reporting requirement for "frequency tables...
 * mean delay by route and cause code, cost per delay category."
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsSummaryDto {

    private long totalDelayEvents;
    private long totalResolvedScenarios;
    private long totalPendingScenarios;

    private Map<String, Long> delayCountByCategory;
    private Map<String, Double> avgDelayMinutesByCategory;
    private Map<String, Double> costByCategory;

    private double totalCostSaved;
    private double totalEstimatedCostOfSelectedOptions;
    private double totalActualCost;

    /** Null when no scenario has a recorded MILP/RL solve time yet. */
    private Double avgComputationTimeMs;

    /** Average minutes between the ranked options appearing and a decision being made. Null if no resolved scenarios. */
    private Double avgDecisionMinutes;

    /**
     * Only populated from scenarios where a dispatcher explicitly entered a manual-baseline
     * estimate. actualCostForBaselineComparison is scoped to the SAME scenario set as
     * manualBaselineTotal (unlike totalActualCost, which covers every recorded outcome) so
     * the two numbers are a fair apples-to-apples comparison.
     */
    private Double manualBaselineTotal;
    private Double actualCostForBaselineComparison;
    private Double manualVsDssSavingsPercent;
    private int scenariosWithManualBaseline;
}
