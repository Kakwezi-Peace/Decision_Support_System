package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private RecoveryScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OptionType optionType;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Rank of this option — 1 is the best/most optimal
     */
    private int rank;

    private double totalCost;

    private double crewOvertimeCost;

    private double fuelCost;

    private double passengerCompensationCost;

    private double slotPenaltyCost;

    private double mroCost;

    /**
     * Estimated delay reduction in minutes if this option is applied
     */
    private int estimatedDelayReduction;

    private boolean feasible;

    @Column(columnDefinition = "TEXT")
    private String feasibilityNotes;

    private boolean isSelected;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GeneratedBy generatedBy;

    /**
     * Option-characterisation fields named in the proposal's Section 2.2.4 ("Each
     * option is characterised by its estimated total recovery cost, projected delay
     * absorption, passenger impact score, crew duty compliance status, and
     * regulatory feasibility") but with no formula given for the last three. MILP
     * and RL options get these computed by the optimizer (see optimizer/option_scoring.py);
     * manually-entered override options are left null/UNKNOWN rather than guessed.
     */
    private Double passengerImpactScore;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private CrewDutyCompliance crewDutyCompliance = CrewDutyCompliance.UNKNOWN;

    private Boolean regulatoryFeasible;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum CrewDutyCompliance {
        COMPLIANT,
        AT_RISK,
        VIOLATION,
        UNKNOWN
    }

    public enum OptionType {
        DELAY_ABSORPTION,
        FLIGHT_CANCELLATION,
        AIRCRAFT_SWAP,
        CREW_SUBSTITUTION,
        PASSENGER_REROUTING,
        COMBINED
    }

    public enum GeneratedBy {
        MILP,
        RL,
        MANUAL
    }
}
