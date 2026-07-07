package rw.ac.dss.dto.optimizer;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mirrors the Python optimizer's expected spare-aircraft dict keys
 * (see optimizer/models.py AircraftInfo and optimizer/optimizer.py _build_aircraft_swap).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftSpareDto {

    private Long id;

    private String type;

    private String location;

    private int seats;

    private String status;

    @JsonProperty("last_maintenance_hours")
    private double lastMaintenanceHours;
}
