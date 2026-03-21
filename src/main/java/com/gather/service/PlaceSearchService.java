package com.gather.service;

import com.gather.model.Place;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Generic interface for place search services.
 * Allows easy switching between different providers (Yelp, Google Places, etc.)
 */
public interface PlaceSearchService {

    /**
     * Search for places based on location and search term
     * @param location The location to search in (e.g., "Seattle, WA")
     * @param term The search term (e.g., "bars", "restaurants")
     * @param limit Maximum number of results to return
     * @return List of places matching the search criteria
     */
    Mono<List<Place>> searchPlaces(String location, String term, int limit);

    /**
     * Get the provider name (e.g., "yelp", "google")
     * @return Provider identifier
     */
    String getProviderName();
}
