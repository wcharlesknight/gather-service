package com.gather.service;

import com.gather.model.GooglePlace;
import com.gather.model.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Google Places implementation of PlaceSearchService.
 * Adapts GooglePlacesApiService to the generic PlaceSearchService interface.
 */
@Service("googlePlaceSearchService")
public class GooglePlaceSearchService implements PlaceSearchService {
    private static final Logger logger = LoggerFactory.getLogger(GooglePlaceSearchService.class);

    private final GooglePlacesApiService googlePlacesApiService;

    public GooglePlaceSearchService(GooglePlacesApiService googlePlacesApiService) {
        this.googlePlacesApiService = googlePlacesApiService;
    }

    @Override
    public Mono<List<Place>> searchPlaces(String location, String term, int limit) {
        logger.info("Searching Google Places for '{}' in '{}'", term, location);

        // Construct natural language query for Google Places
        String textQuery = term + " in " + location;

        return googlePlacesApiService.searchPlaces(textQuery, limit)
                .map(response -> {
                    if (response == null || response.getPlaces() == null) {
                        return Collections.<Place>emptyList();
                    }
                    return response.getPlaces().stream()
                            .map(this::convertToPlace)
                            .collect(Collectors.toList());
                })
                .doOnSuccess(places -> logger.info("Converted {} Google Places to generic Place objects", places.size()))
                .onErrorResume(error -> {
                    logger.error("Error searching Google Places: {}", error.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    @Override
    public String getProviderName() {
        return "google";
    }

    /**
     * Convert GooglePlace to generic Place model
     */
    private Place convertToPlace(GooglePlace googlePlace) {
        Place place = new Place();
        place.setProviderId(googlePlace.getId());
        place.setProvider("google");
        place.setName(googlePlace.getDisplayName() != null ? googlePlace.getDisplayName().getText() : null);
        place.setAddress(googlePlace.getFormattedAddress());
        place.setRating(googlePlace.getRating());
        place.setReviewCount(googlePlace.getUserRatingCount());
        place.setPhoneNumber(googlePlace.getNationalPhoneNumber());
        place.setUrl(googlePlace.getGoogleMapsUri());

        if (googlePlace.getLocation() != null) {
            place.setLatitude(googlePlace.getLocation().getLatitude());
            place.setLongitude(googlePlace.getLocation().getLongitude());
        }

        place.setPriceLevel(googlePlace.getPriceLevel());

        return place;
    }
}
