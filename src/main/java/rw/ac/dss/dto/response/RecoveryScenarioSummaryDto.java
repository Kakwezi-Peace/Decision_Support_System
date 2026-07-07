package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.DelayEvent;
import rw.ac.dss.model.RecoveryOption;
import rw.ac.dss.model.RecoveryScenario;

import java.time.LocalDateTime;

/**
 * Row shape for the Recovery History page - the proposal's "searchable record of
 * recovery decisions and their outcomes, supporting post-operational analysis,
 * controller training, and continuous improvement of OCC procedures."
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryScenarioSummaryDto {

    private Long id;
    private Long delayEventId;
    private String flightNumber;
    private String origin;
    private String destination;
    private DelayEvent.DelayCategory delayCategory;
    private int delayMinutes;
    private RecoveryScenario.ScenarioStatus status;
    private RecoveryScenario.ScenarioPriority priority;
    private LocalDateTime optimisationRunAt;
    private LocalDateTime resolvedAt;
    private String decisionMadeBy;
    private RecoveryOption.OptionType selectedOptionType;
    private Double selectedOptionCost;
    private Boolean outcomeRecorded;
    private Double actualCost;
    private Double actualCostSaved;
    private Double manualBaselineCost;

    public static RecoveryScenarioSummaryDto from(RecoveryScenario s, RecoveryOption selected) {
        DelayEvent delayEvent = s.getDelayEvent();
        return RecoveryScenarioSummaryDto.builder()
                .id(s.getId())
                .delayEventId(delayEvent != null ? delayEvent.getId() : null)
                .flightNumber(delayEvent != null && delayEvent.getFlight() != null ? delayEvent.getFlight().getFlightNumber() : null)
                .origin(delayEvent != null && delayEvent.getFlight() != null ? delayEvent.getFlight().getOrigin() : null)
                .destination(delayEvent != null && delayEvent.getFlight() != null ? delayEvent.getFlight().getDestination() : null)
                .delayCategory(delayEvent != null ? delayEvent.getDelayCategory() : null)
                .delayMinutes(delayEvent != null ? delayEvent.getDelayMinutes() : 0)
                .status(s.getStatus())
                .priority(s.getPriority())
                .optimisationRunAt(s.getOptimisationRunAt())
                .resolvedAt(s.getResolvedAt())
                .decisionMadeBy(s.getDecisionMadeBy())
                .selectedOptionType(selected != null ? selected.getOptionType() : null)
                .selectedOptionCost(selected != null ? selected.getTotalCost() : null)
                .outcomeRecorded(s.isOutcomeRecorded())
                .actualCost(s.isOutcomeRecorded() ? s.getActualCost() : null)
                .actualCostSaved(s.isOutcomeRecorded() ? s.getActualCostSaved() : null)
                .manualBaselineCost(s.getManualBaselineCost())
                .build();
    }
}
