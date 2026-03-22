package com.gather.model.provider.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlace {
    private String id;
    private DisplayName displayName;
    private String formattedAddress;
    private Location location;
    private Double rating;
    private Integer userRatingCount;
    private String nationalPhoneNumber;
    private String googleMapsUri;
    private String priceLevel;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        private String text;
        private String languageCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private Double latitude;
        private Double longitude;
    }
}
