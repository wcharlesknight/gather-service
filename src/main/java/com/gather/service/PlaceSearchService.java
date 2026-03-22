package com.gather.service;

import com.gather.model.domain.Place;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Generic interface for place search services.
 * Allows easy switching between different providers (Google Places, etc.)
 */
public interface PlaceSearchService {
    Mono<List<Place>> searchPlaces(String location, String term, int limit);
    String getProviderName();
}
