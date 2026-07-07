package rw.ac.dss.dto.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class RecordOutcomeRequest {

    @PositiveOrZero
    private double actualCost;

    /** Optional: what this recovery would have cost under the prior manual approach, for baseline comparison. */
    @PositiveOrZero
    private Double manualBaselineCost;
}
