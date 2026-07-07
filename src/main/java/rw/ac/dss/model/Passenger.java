package rw.ac.dss.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "passengers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String bookingReference;

    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketClass ticketClass;

    private boolean hasConnection;

    private String connectionFlight;

    private int connectionDeadlineMinutes;

    private boolean reAccommodated;

    private double compensationPaid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PassengerStatus status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum TicketClass {
        ECONOMY,
        BUSINESS,
        FIRST
    }

    public enum PassengerStatus {
        CHECKED_IN,
        BOARDED,
        DISRUPTED,
        REACCOMMODATED
    }
}
