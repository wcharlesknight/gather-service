package com.gather.model.domain;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.firestore.annotation.IgnoreExtraProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@IgnoreExtraProperties
public class UserProfile {

    @DocumentId
    private String uid;
    private String displayName;
    private String email;
    private String fcmToken;
    private UserLocation location;

    @Data
    @NoArgsConstructor
    @IgnoreExtraProperties
    public static class UserLocation {
        private String cityId;
    }
}
