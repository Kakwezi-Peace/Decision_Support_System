package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "delay_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelayEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    /**
     * IATA SSIM delay code e.g. 93 (ATC), 41 (Technical), 11 (Late Passengers)
     */
    private String delayCode;

    @Column(columnDefinition = "TEXT")
    private String delayCause;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelayCategory delayCategory;

    private int delayMinutes;

    private double estimatedDirectCost;

    private double estimatedPassengerCost;

    private double totalEstimatedCost;

    private LocalDateTime reportedAt;

    private String reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DelayStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum DelayCategory {
        TECHNICAL,
        WEATHER,
        ATC,
        CREW,
        COMMERCIAL,
        REACTIONARY,
        OTHER
    }

    public enum DelayStatus {
        ACTIVE,
        RECOVERING,
        RESOLVED
    }
}
