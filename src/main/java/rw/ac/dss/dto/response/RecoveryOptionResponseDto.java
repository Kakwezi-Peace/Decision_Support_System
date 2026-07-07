package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.RecoveryOption;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryOptionResponseDto {

    private Long id;
    private RecoveryOption.OptionType optionType;
    private String description;
    private int rank;
    private double totalCost;
    private double crewOvertimeCost;
    private double fuelCost;
    private double passengerCompensationCost;
    private double slotPenaltyCost;
    private double mroCost;
    private int estimatedDelayReduction;
    private boolean feasible;
    private String feasibilityNotes;
    private boolean selected;
    private RecoveryOption.GeneratedBy generatedBy;
    private Double passengerImpactScore;
    private RecoveryOption.CrewDutyCompliance crewDutyCompliance;
    private Boolean regulatoryFeasible;

    public static RecoveryOptionResponseDto from(RecoveryOption o) {
        return RecoveryOptionResponseDto.builder()
                .id(o.getId())
                .optionType(o.getOptionType())
                .description(o.getDescription())
                .rank(o.getRank())
                .totalCost(o.getTotalCost())
                .crewOvertimeCost(o.getCrewOvertimeCost())
                .fuelCost(o.getFuelCost())
                .passengerCompensationCost(o.getPassengerCompensationCost())
                .slotPenaltyCost(o.getSlotPenaltyCost())
                .mroCost(o.getMroCost())
                .estimatedDelayReduction(o.getEstimatedDelayReduction())
                .feasible(o.isFeasible())
                .feasibilityNotes(o.getFeasibilityNotes())
                .selected(o.isSelected())
                .generatedBy(o.getGeneratedBy())
                .passengerImpactScore(o.getPassengerImpactScore())
                .crewDutyCompliance(o.getCrewDutyCompliance())
                .regulatoryFeasible(o.getRegulatoryFeasible())
                .build();
    }
}
