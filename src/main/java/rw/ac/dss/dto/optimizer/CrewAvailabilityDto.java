package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the Python optimizer's expected crew dict keys
 * (see optimizer/models.py CrewMember).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewAvailabilityDto {

    private Long id;

    private String role;

    private String qualification;

    @JsonProperty("duty_hours_remaining")
    private double dutyHoursRemaining;

    @JsonProperty("overtime_rate")
    private double overtimeRate;
}
