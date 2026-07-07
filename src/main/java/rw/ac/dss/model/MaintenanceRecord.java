package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "maintenance_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceType maintenanceType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime startTime;

    private LocalDateTime estimatedEndTime;

    private LocalDateTime actualEndTime;

    /**
     * Location of maintenance e.g. KIA Kigali, Nairobi MRO
     */
    private String location;

    private String technicianName;

    private String partnerFacility;

    private double estimatedCost;

    private double actualCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaintenanceStatus status;

    private boolean affectsOperations;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum MaintenanceType {
        ROUTINE_CHECK,
        A_CHECK,
        B_CHECK,
        C_CHECK,
        D_CHECK,
        AOG_REPAIR,
        COMPONENT_REPLACEMENT
    }

    public enum MaintenanceStatus {
        SCHEDULED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}
