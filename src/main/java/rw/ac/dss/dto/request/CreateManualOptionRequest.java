package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import rw.ac.dss.model.RecoveryOption;

/**
 * Lets an operations controller add a recovery option the model didn't generate -
 * the proposal's "structured override mechanism ... to incorporate tacit operational
 * knowledge not captured in the model" (Section 2.2.4, Decision Interface).
 */
@Data
public class CreateManualOptionRequest {

    @NotNull
    private RecoveryOption.OptionType optionType;

    @NotBlank
    private String description;

    @PositiveOrZero
    private double totalCost;

    @PositiveOrZero
    private double crewOvertimeCost;

    @PositiveOrZero
    private double fuelCost;

    @PositiveOrZero
    private double passengerCompensationCost;

    @PositiveOrZero
    private double slotPenaltyCost;

    @PositiveOrZero
    private double mroCost;

    private int estimatedDelayReduction;

    private String feasibilityNotes;

    private String addedBy;
}
