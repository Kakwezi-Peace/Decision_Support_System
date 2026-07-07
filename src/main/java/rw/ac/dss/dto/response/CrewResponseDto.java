package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.Crew;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CrewResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String employeeId;
    private Crew.CrewRole role;
    private String qualifications;
    private double dutyHoursUsed;
    private double maxDutyHours;
    private String currentLocation;
    private Crew.CrewStatus status;
    private double overtimeRate;

    public static CrewResponseDto from(Crew c) {
        return CrewResponseDto.builder()
                .id(c.getId())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .employeeId(c.getEmployeeId())
                .role(c.getRole())
                .qualifications(c.getQualifications())
                .dutyHoursUsed(c.getDutyHoursUsed())
                .maxDutyHours(c.getMaxDutyHours())
                .currentLocation(c.getCurrentLocation())
                .status(c.getStatus())
                .overtimeRate(c.getOvertimeRate())
                .build();
    }
}
