package com.gather.service;

import com.gather.config.YelpApiConfig;
import com.gather.model.YelpSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class YelpApiService {
    private static final Logger logger = LoggerFactory.getLogger(YelpApiService.class);

    private final WebClient webClient;
    private final YelpApiConfig yelpApiConfig;

    @Value("${yelp.job.search-limit:50}")
    private int searchLimit;

    public YelpApiService(WebClient.Builder webClientBuilder, YelpApiConfig yelpApiConfig) {
        this.yelpApiConfig = yelpApiConfig;
        this.webClient = webClientBuilder
                .baseUrl(yelpApiConfig.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + yelpApiConfig.getApiKey())
                .build();
    }

    /**
     * Search for businesses using Yelp API - returns structured response
     * @param location The location to search in
     * @param term The search term (e.g., "restaurants", "coffee")
     * @param limit Number of results to return
     * @return Structured Yelp search response
     */
    public Mono<YelpSearchResponse> searchBusinesses(String location, String term, int limit) {
        logger.info("Searching Yelp for term: {} in location: {}, limit: {}", term, location, limit);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/businesses/search")
                        .queryParam("location", location)
                        .queryParam("term", term)
                        .queryParam("limit", limit)
                        .build())
                .retrieve()
                .bodyToMono(YelpSearchResponse.class)
                .doOnSuccess(response -> logger.info("Successfully retrieved {} businesses from Yelp",
                        response.getBusinesses() != null ? response.getBusinesses().size() : 0))
                .doOnError(error -> logger.error("Error calling Yelp API: {}", error.getMessage()));
    }

    /**
     * Search for businesses using Yelp API - returns raw JSON string
     * @param location The location to search in
     * @param term The search term
     * @return Response from Yelp API as JSON string
     */
    public Mono<String> searchBusinessesRaw(String location, String term) {
        logger.info("Searching Yelp for term: {} in location: {}", term, location);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/businesses/search")
                        .queryParam("location", location)
                        .queryParam("term", term)
                        .queryParam("limit", 20)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.debug("Successfully retrieved businesses from Yelp"))
                .doOnError(error -> logger.error("Error calling Yelp API: {}", error.getMessage()));
    }

    /**
     * Get business details by ID
     * @param businessId The Yelp business ID
     * @return Business details from Yelp API
     */
    public Mono<String> getBusinessDetails(String businessId) {
        logger.info("Getting business details for ID: {}", businessId);

        return webClient.get()
                .uri("/businesses/{id}", businessId)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.debug("Successfully retrieved business details"))
                .doOnError(error -> logger.error("Error getting business details: {}", error.getMessage()));
    }
}
