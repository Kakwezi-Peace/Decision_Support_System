package rw.ac.dss.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * No email service is configured for this project, so the reset token is handed
 * back directly in the response instead of being emailed - the frontend renders it
 * as a clickable link. Not how a production system would deliver this.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordResponseDto {

    private String resetToken;
    private LocalDateTime expiresAt;
}
