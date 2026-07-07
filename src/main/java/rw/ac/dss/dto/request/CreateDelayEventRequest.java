package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import rw.ac.dss.model.DelayEvent;

@Data
public class CreateDelayEventRequest {

    @NotNull
    private Long flightId;

    private String delayCode;

    private String delayCause;

    @NotNull
    private DelayEvent.DelayCategory delayCategory;

    @Positive
    private int delayMinutes;

    private double estimatedDirectCost;

    private double estimatedPassengerCost;

    private double totalEstimatedCost;

    private String reportedBy;

    private String notes;
}
