package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mirrors the OCC staff categories in the research proposal's Table 1
     * (Population and Sample Size Distribution), plus ADMIN for system
     * administration. Each non-admin role owns exactly one data domain and can
     * only read elsewhere - see SecurityConfig for the enforced matrix.
     */
    public enum Role {
        ADMIN,
        OPERATIONS_CONTROLLER,
        CREW_SCHEDULER,
        MAINTENANCE_CONTROLLER,
        COMMERCIAL_SERVICES,
        SENIOR_MANAGEMENT
    }
}
