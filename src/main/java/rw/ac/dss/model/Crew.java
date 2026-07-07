package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrewRole role;

    /**
     * Comma-separated aircraft type qualifications, e.g. "B737,A330"
     */
    @Column(nullable = false)
    private String qualifications;

    private double dutyHoursUsed;

    @Column(nullable = false)
    @Builder.Default
    private double maxDutyHours = 14.0;

    private String currentLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrewStatus status;

    private double overtimeRate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum CrewRole {
        CAPTAIN,
        FIRST_OFFICER,
        SENIOR_FA,
        FLIGHT_ATTENDANT
    }

    public enum CrewStatus {
        AVAILABLE,
        ON_DUTY,
        ON_REST,
        ON_LEAVE,
        SICK
    }
}
