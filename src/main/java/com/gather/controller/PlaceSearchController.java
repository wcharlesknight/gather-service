package com.gather.controller;

import com.gather.model.domain.Place;
import com.gather.service.GooglePlaceSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/places")
public class PlaceSearchController {
    private static final Logger logger = LoggerFactory.getLogger(PlaceSearchController.class);

    private final GooglePlaceSearchService googlePlaceSearchService;

    public PlaceSearchController(GooglePlaceSearchService googlePlaceSearchService) {
        this.googlePlaceSearchService = googlePlaceSearchService;
    }

    private static final int MAX_LIMIT = 50;

    @GetMapping("/search")
    public Mono<List<Place>> searchPlaces(
            @RequestParam String location,
            @RequestParam String term,
            @RequestParam(defaultValue = "20") int limit) {
        // Clamp limit: this endpoint proxies a billable upstream API.
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        logger.info("Searching for '{}' in '{}'", term, location);
        return googlePlaceSearchService.searchPlaces(location, term, safeLimit);
    }
}
