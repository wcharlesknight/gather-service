package com.gather.controller;

import com.gather.service.GooglePlacesApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/google-places")
public class GooglePlacesController {
    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesController.class);

    private final GooglePlacesApiService googlePlacesApiService;

    public GooglePlacesController(GooglePlacesApiService googlePlacesApiService) {
        this.googlePlacesApiService = googlePlacesApiService;
    }

    /**
     * Search for places
     * GET /api/google-places/search?query=bars+in+Seattle
     */
    @GetMapping("/search")
    public Mono<ResponseEntity<String>> searchPlaces(
            @RequestParam String query) {

        logger.info("Received search request for query: {}", query);

        return googlePlacesApiService.searchPlacesRaw(query)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error in search endpoint", error);
                    return Mono.just(ResponseEntity.internalServerError().body("Error: " + error.getMessage()));
                });
    }

    /**
     * Get place details by ID
     * GET /api/google-places/place/{id}
     */
    @GetMapping("/place/{id}")
    public Mono<ResponseEntity<String>> getPlaceDetails(@PathVariable String id) {
        logger.info("Received request for place ID: {}", id);

        return googlePlacesApiService.getPlaceDetails(id)
                .map(ResponseEntity::ok)
                .onErrorResume(error -> {
                    logger.error("Error getting place details", error);
                    return Mono.just(ResponseEntity.internalServerError().body("Error: " + error.getMessage()));
                });
    }
}
