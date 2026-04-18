package com.gather.model.domain;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GatheringSpot {
    @DocumentId
    private String id;
    private String cityId;
    private String provider;
    private String googlePlaceId;
    private String businessName;
    private String address;
    private Double rating;
    private Long selectedAt;
    private Boolean notificationSent;
    private Long notificationSentAt;
    private String phoneNumber;
    private String url;

    public GatheringSpot(String cityId, Place place) {
        this.cityId = cityId;
        this.provider = place.getProvider();
        if ("google".equals(place.getProvider())) {
            this.googlePlaceId = place.getProviderId();
        }
        this.businessName = place.getName();
        this.address = place.getAddress();
        this.rating = place.getRating();
        this.selectedAt = System.currentTimeMillis();
        this.notificationSent = false;
        this.phoneNumber = place.getPhoneNumber();
        this.url = place.getUrl();
    }
}
