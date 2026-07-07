package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors optimizer/models.py FeedbackResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackResponseDto {

    private boolean updated;
    private double reward;

    @JsonProperty("old_q_value")
    private Double oldQValue;

    @JsonProperty("new_q_value")
    private Double newQValue;

    private String message;
}
