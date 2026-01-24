package com.gather.controller;

import com.gather.service.YelpApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/yelp")
public class YelpController {
    private static final Logger logger = LoggerFactory.getLogger(YelpController.class);

    private final YelpApiService yelpApiService;

    public YelpController(YelpApiService yelpApiService) {
        this.yelpApiService = yelpApiService;
    }

    /**
     * Search for businesses
     * GET /api/yelp/search?location=San Francisco&term=restaurants
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<String>> searchBusinesses(
            @RequestParam String location,
            @RequestParam(required = false, defaultValue = "restaurants") String term) {

        logger.info("Received search request for location: {}, term: {}", location, term);

        return yelpApiService.searchBusinessesRaw(location, term)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error in search endpoint", error);
                    return Mono.just(ResponseEntity.internalServerError().body("Error: " + error.getMessage()));
                });
    }

    /**
     * Get business details by ID
     * GET /api/yelp/business/{id}
     */
    @GetMapping("/business/{id}")
    public Mono<ResponseEntity<String>> getBusinessDetails(@PathVariable String id) {
        logger.info("Received request for business ID: {}", id);

        return yelpApiService.getBusinessDetails(id)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error getting business details", error);
                    return Mono.just(ResponseEntity.internalServerError().body("Error: " + error.getMessage()));
                });
    }
}
