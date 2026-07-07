package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors optimizer/models.py FeedbackRequest.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequestDto {

    @JsonProperty("optimisation_request")
    private OptimizeRequestDto optimisationRequest;

    @JsonProperty("chosen_action")
    private String chosenAction;

    @JsonProperty("estimated_cost")
    private double estimatedCost;

    @JsonProperty("actual_cost")
    private double actualCost;
}
