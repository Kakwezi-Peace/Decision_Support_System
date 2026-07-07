package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.ac.dss.model.DelayEvent;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelayEventResponseDto {

    private Long id;
    private Long flightId;
    private String flightNumber;
    private DelayEvent.DelayCategory delayCategory;
    private int delayMinutes;
    private String delayCause;
    private String reportedBy;
    private DelayEvent.DelayStatus status;
    private LocalDateTime reportedAt;

    public static DelayEventResponseDto from(DelayEvent e) {
        return DelayEventResponseDto.builder()
                .id(e.getId())
                .flightId(e.getFlight() != null ? e.getFlight().getId() : null)
                .flightNumber(e.getFlight() != null ? e.getFlight().getFlightNumber() : null)
                .delayCategory(e.getDelayCategory())
                .delayMinutes(e.getDelayMinutes())
                .delayCause(e.getDelayCause())
                .reportedBy(e.getReportedBy())
                .status(e.getStatus())
                .reportedAt(e.getReportedAt())
                .build();
    }
}
