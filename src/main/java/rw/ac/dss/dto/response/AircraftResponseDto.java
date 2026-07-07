package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.Aircraft;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AircraftResponseDto {

    private Long id;
    private String registrationNumber;
    private Aircraft.AircraftType aircraftType;
    private Aircraft.AircraftStatus status;
    private String currentLocation;
    private int totalSeats;
    private int yearManufactured;
    private LocalDate lastMaintenanceDate;

    public static AircraftResponseDto from(Aircraft a) {
        return AircraftResponseDto.builder()
                .id(a.getId())
                .registrationNumber(a.getRegistrationNumber())
                .aircraftType(a.getAircraftType())
                .status(a.getStatus())
                .currentLocation(a.getCurrentLocation())
                .totalSeats(a.getTotalSeats())
                .yearManufactured(a.getYearManufactured())
                .lastMaintenanceDate(a.getLastMaintenanceDate())
                .build();
    }
}
