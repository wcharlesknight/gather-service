package com.gather.service;

import com.gather.config.GooglePlacesApiConfig;
import com.gather.model.provider.google.GooglePlacesSearchRequest;
import com.gather.model.provider.google.GooglePlacesSearchResponse;
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
}
