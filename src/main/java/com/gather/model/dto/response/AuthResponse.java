package com.gather.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String customToken;
    private String uid;
    private String displayName;
    private String email;
    /** True when this login created the user's profile for the first time (e.g. a new social sign-in). */
    private boolean isNewUser;
}
