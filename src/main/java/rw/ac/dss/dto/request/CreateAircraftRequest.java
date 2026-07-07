package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import rw.ac.dss.model.Aircraft;

import java.time.LocalDate;

@Data
public class CreateAircraftRequest {

    @NotBlank
    private String registrationNumber;

    @NotNull
    private Aircraft.AircraftType aircraftType;

    @NotNull
    private Aircraft.AircraftStatus status;

    @NotBlank
    private String currentLocation;

    @Positive
    private int totalSeats;

    private int yearManufactured;

    private LocalDate lastMaintenanceDate;

    private String notes;
}
