package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Mirrors optimizer/models.py OptimisationResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResponseDto {

    @JsonProperty("delay_event_id")
    private Long delayEventId;

    private List<RecoveryOptionResultDto> options;

    @JsonProperty("computation_time_ms")
    private double computationTimeMs;

    @JsonProperty("recommended_option_index")
    private int recommendedOptionIndex;
}
