package rw.ac.dss.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import rw.ac.dss.model.User;

@Data
public class UpdateUserRequest {

    private String fullName;

    @NotNull
    private User.Role role;

    private boolean enabled = true;

    /**
     * Optional - leave null/blank to keep the existing password unchanged. Length is
     * checked in AuthService (not here via @Size) since blank must mean "no change",
     * not "invalid".
     */
    private String password;
}
