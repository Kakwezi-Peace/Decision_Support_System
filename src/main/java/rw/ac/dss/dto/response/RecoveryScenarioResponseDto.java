package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.RecoveryScenario;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryScenarioResponseDto {

    private Long id;
    private Long delayEventId;
    private String scenarioTitle;
    private RecoveryScenario.ScenarioStatus status;
    private RecoveryScenario.ScenarioPriority priority;
    private LocalDateTime optimisationRunAt;
    private Long selectedOptionId;
    private String decisionMadeBy;
    private LocalDateTime resolvedAt;
    private double computationTimeMs;
    private List<RecoveryOptionResponseDto> options;

    /** Populated once an outcome has been recorded (Section 2.2.4's Feedback and Learning Loop). */
    private Double actualCost;
    private Double actualCostSaved;
    private Double manualBaselineCost;

    /** Only set as part of the record-outcome response - not persisted, purely informational. */
    private Boolean rlPolicyUpdated;
    private String rlFeedbackMessage;

    public static RecoveryScenarioResponseDto from(RecoveryScenario s, List<RecoveryOptionResponseDto> options, Double computationTimeMs) {
        return RecoveryScenarioResponseDto.builder()
                .id(s.getId())
                .delayEventId(s.getDelayEvent() != null ? s.getDelayEvent().getId() : null)
                .scenarioTitle(s.getScenarioTitle())
                .status(s.getStatus())
                .priority(s.getPriority())
                .optimisationRunAt(s.getOptimisationRunAt())
                .selectedOptionId(s.getSelectedOptionId())
                .decisionMadeBy(s.getDecisionMadeBy())
                .resolvedAt(s.getResolvedAt())
                .computationTimeMs(computationTimeMs != null ? computationTimeMs : 0.0)
                .options(options)
                .actualCost(s.isOutcomeRecorded() ? s.getActualCost() : null)
                .actualCostSaved(s.isOutcomeRecorded() ? s.getActualCostSaved() : null)
                .manualBaselineCost(s.getManualBaselineCost())
                .build();
    }
}
