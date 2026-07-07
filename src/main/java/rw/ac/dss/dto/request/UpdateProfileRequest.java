package rw.ac.dss.dto.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;

    /** Required only if newPassword is provided - verified against the current hash. */
    private String currentPassword;

    /** Optional - leave blank to keep the existing password unchanged. */
    private String newPassword;
}
