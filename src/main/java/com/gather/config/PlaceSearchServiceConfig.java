package com.gather.config;

import com.gather.service.GooglePlaceSearchService;
import com.gather.service.PlaceSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaceSearchServiceConfig {
    private static final Logger logger = LoggerFactory.getLogger(PlaceSearchServiceConfig.class);

    @Value("${place-service.active-provider:google}")
    private String activeProvider;

    private final GooglePlaceSearchService googlePlaceSearchService;

    public PlaceSearchServiceConfig(GooglePlaceSearchService googlePlaceSearchService) {
        this.googlePlaceSearchService = googlePlaceSearchService;
    }

    @Bean(name = "activePlaceSearchService")
    public PlaceSearchService activePlaceSearchService() {
        if ("google".equalsIgnoreCase(activeProvider)) {
            logger.info("Using Google Places as the active place search provider");
            return googlePlaceSearchService;
        } else {
            logger.warn("Unknown provider '{}', defaulting to Google Places", activeProvider);
            return googlePlaceSearchService;
        }
    }
}
