package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors optimizer/models.py RecoveryOptionResult.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryOptionResultDto {

    @JsonProperty("option_type")
    private String optionType;

    private String description;

    private int rank;

    @JsonProperty("total_cost")
    private double totalCost;

    @JsonProperty("crew_overtime_cost")
    private double crewOvertimeCost;

    @JsonProperty("fuel_cost")
    private double fuelCost;

    @JsonProperty("passenger_compensation_cost")
    private double passengerCompensationCost;

    @JsonProperty("slot_penalty_cost")
    private double slotPenaltyCost;

    @JsonProperty("mro_cost")
    private double mroCost;

    @JsonProperty("estimated_delay_reduction")
    private int estimatedDelayReduction;

    private boolean feasible;

    @JsonProperty("feasibility_notes")
    private String feasibilityNotes;

    @JsonProperty("generated_by")
    private String generatedBy;

    @JsonProperty("passenger_impact_score")
    private double passengerImpactScore;

    @JsonProperty("crew_duty_compliance")
    private String crewDutyCompliance;

    @JsonProperty("regulatory_feasible")
    private boolean regulatoryFeasible;
}
