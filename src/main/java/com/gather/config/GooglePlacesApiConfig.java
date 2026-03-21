package com.gather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "google.places.api")
public class GooglePlacesApiConfig {
    private String apiKey;
    private String baseUrl = "https://places.googleapis.com/v1";
    private String defaultFieldMask = "places.id,places.displayName,places.formattedAddress,places.location,places.rating,places.userRatingCount,places.nationalPhoneNumber,places.googleMapsUri,places.priceLevel";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultFieldMask() {
        return defaultFieldMask;
    }

    public void setDefaultFieldMask(String defaultFieldMask) {
        this.defaultFieldMask = defaultFieldMask;
    }
}
