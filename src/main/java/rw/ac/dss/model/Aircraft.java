package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "aircraft")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registrationNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftType aircraftType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AircraftStatus status;

    @Column(nullable = false)
    private String currentLocation;

    private int totalSeats;

    private int yearManufactured;

    private LocalDate lastMaintenanceDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AircraftType {
        BOEING_737,
        AIRBUS_A330,
        TURBOPROP_Q400
    }

    public enum AircraftStatus {
        SERVICEABLE,
        UNSERVICEABLE,
        IN_MAINTENANCE
    }
}
