package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rw.ac.dss.model.Crew;

@Data
public class CreateCrewRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String employeeId;

    @NotNull
    private Crew.CrewRole role;

    /** Comma-separated aircraft type qualifications, e.g. "B737,A330" */
    @NotBlank
    private String qualifications;

    private double dutyHoursUsed;

    private Double maxDutyHours;

    private String currentLocation;

    @NotNull
    private Crew.CrewStatus status;

    private double overtimeRate;
}
