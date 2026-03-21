package com.gather.service;

import com.gather.config.GooglePlacesApiConfig;
import com.gather.model.GooglePlacesSearchRequest;
import com.gather.model.GooglePlacesSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GooglePlacesApiService {
    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesApiService.class);

    private final WebClient webClient;
    private final GooglePlacesApiConfig config;

    public GooglePlacesApiService(WebClient.Builder webClientBuilder, GooglePlacesApiConfig config) {
        this.config = config;
        this.webClient = webClientBuilder
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("X-Goog-Api-Key", config.getApiKey())
                .defaultHeader("X-Goog-FieldMask", config.getDefaultFieldMask())
                .build();
    }

    /**
     * Search for places using Google Places API - returns structured response
     * @param textQuery The search query (e.g., "bars in Seattle, WA")
     * @param maxResults Number of results to return (max 20)
     * @return Structured Google Places search response
     */
    public Mono<GooglePlacesSearchResponse> searchPlaces(String textQuery, int maxResults) {
        logger.info("Searching Google Places for query: {}, maxResults: {}", textQuery, maxResults);

        GooglePlacesSearchRequest request = new GooglePlacesSearchRequest(textQuery, maxResults);

        return webClient.post()
                .uri("/places:searchText")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GooglePlacesSearchResponse.class)
                .doOnSuccess(response -> logger.info("Successfully retrieved {} places from Google Places API",
                        response.getPlaces() != null ? response.getPlaces().size() : 0))
                .doOnError(error -> logger.error("Error calling Google Places API: {}", error.getMessage()));
    }

    /**
     * Search for places using Google Places API - returns raw JSON string
     * @param textQuery The search query
     * @return Response from Google Places API as JSON string
     */
    public Mono<String> searchPlacesRaw(String textQuery) {
        logger.info("Searching Google Places for query: {}", textQuery);

        GooglePlacesSearchRequest request = new GooglePlacesSearchRequest(textQuery);

        return webClient.post()
                .uri("/places:searchText")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.debug("Successfully retrieved places from Google Places API"))
                .doOnError(error -> logger.error("Error calling Google Places API: {}", error.getMessage()));
    }

    /**
     * Get place details by ID
     * @param placeId The Google Place ID
     * @return Place details from Google Places API
     */
    public Mono<String> getPlaceDetails(String placeId) {
        logger.info("Getting place details for ID: {}", placeId);

        return webClient.get()
                .uri("/places/{id}", placeId)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.debug("Successfully retrieved place details"))
                .doOnError(error -> logger.error("Error getting place details: {}", error.getMessage()));
    }
}
