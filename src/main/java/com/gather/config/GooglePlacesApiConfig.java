package com.gather.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "google.places.api")
public class GooglePlacesApiConfig {
    private String apiKey;
    private String baseUrl = "https://places.googleapis.com/v1";
    private String defaultFieldMask = "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.nationalPhoneNumber,places.googleMapsUri";
}
