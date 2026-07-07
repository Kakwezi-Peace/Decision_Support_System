package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recovery_scenarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecoveryScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delay_event_id", nullable = false)
    private DelayEvent delayEvent;

    @Column(nullable = false)
    private String scenarioTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioPriority priority;

    private LocalDateTime optimisationRunAt;

    private Long selectedOptionId;

    private double actualCost;

    private double actualCostSaved;

    /**
     * Optional dispatcher-entered estimate of what this recovery would have cost
     * under RwandAir's prior manual decision-making process, captured alongside the
     * real outcome so the DSS's actual cost can be compared against it (the
     * proposal's "evaluate the DSS against the current manual approach" objective).
     * Null when not provided - never fabricated by the system itself.
     */
    private Double manualBaselineCost;

    /** Solve time of the most recent MILP/RL optimisation run, for decision-speed analytics. Null until first run. */
    private Double lastComputationTimeMs;

    @Builder.Default
    private boolean outcomeRecorded = false;

    private String decisionMadeBy;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ScenarioStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum ScenarioPriority {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
