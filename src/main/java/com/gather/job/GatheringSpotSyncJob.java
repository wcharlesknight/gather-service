package com.gather.job;

import com.gather.model.City;
import com.gather.model.GatheringSpot;
import com.gather.model.Place;
import com.gather.repository.CityRepository;
import com.gather.repository.GatheringSpotRepository;
import com.gather.service.PlaceSearchService;
import com.gather.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Generic gathering spot selection job that works with any place search provider.
 * The active provider is selected via configuration (place-service.active-provider).
 */
@Component
public class GatheringSpotSyncJob {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotSyncJob.class);

    private final PlaceSearchService placeSearchService;
    private final PushNotificationService pushNotificationService;
    private final CityRepository cityRepository;
    private final GatheringSpotRepository gatheringSpotRepository;
    private final Random random = new Random();

    @Value("${place-service.job.enabled:true}")
    private boolean jobEnabled;

    @Value("${place-service.job.default-location:Seattle, WA}")
    private String defaultLocation;

    @Value("${place-service.job.default-term:bars}")
    private String defaultTerm;

    @Value("${place-service.job.search-limit:50}")
    private int searchLimit;

    @Value("${firebase.notification-topic:weekly-gather}")
    private String notificationTopic;

    @Value("${place-service.job.avoid-repeat-weeks:12}")
    private int avoidRepeatWeeks;

    public GatheringSpotSyncJob(@Qualifier("activePlaceSearchService") PlaceSearchService placeSearchService,
                               PushNotificationService pushNotificationService,
                               CityRepository cityRepository,
                               GatheringSpotRepository gatheringSpotRepository) {
        this.placeSearchService = placeSearchService;
        this.pushNotificationService = pushNotificationService;
        this.cityRepository = cityRepository;
        this.gatheringSpotRepository = gatheringSpotRepository;
    }

    /**
     * Scheduled job that runs weekly to select a gathering spot and notify users
     * Default: Every Thursday at 9:00 AM (notification sent Thursday for Friday gathering)
     * Cron format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "${place-service.job.cron:0 0 9 * * THU}")
    public void selectWeeklyGatheringSpot() {
        if (!jobEnabled) {
            logger.debug("Weekly gathering spot job is disabled");
            return;
        }

        logger.info("Starting weekly gathering spot selection job using provider: {}",
                placeSearchService.getProviderName());

        try {
            // Check if we have cities configured in Firestore
            List<City> enabledCities = cityRepository.findAllEnabled();

            if (!enabledCities.isEmpty()) {
                // Use cities from Firestore
                logger.info("Found {} enabled cities in Firestore", enabledCities.size());
                for (City city : enabledCities) {
                    processCity(city);
                }
            } else {
                // Fallback to default configuration for initial setup
                logger.info("No cities found in Firestore, using default configuration");
                processDefaultCity();
            }
        } catch (Exception e) {
            logger.error("Exception during weekly gathering spot job", e);
        }
    }

    /**
     * Process gathering spot selection for a specific city from Firestore
     */
    private void processCity(City city) {
        logger.info("Processing gathering spot for city: {} using {}", city.getName(),
                placeSearchService.getProviderName());

        placeSearchService.searchPlaces(city.getLocation(), city.getSearchTerm(), city.getSearchLimit())
                .subscribe(
                        places -> processAndNotify(places, city.getId(), city.getTopic()),
                        error -> logger.error("Error during gathering spot job for {}: {}",
                                city.getName(), error.getMessage())
                );
    }

    /**
     * Process with default configuration (fallback)
     */
    private void processDefaultCity() {
        placeSearchService.searchPlaces(defaultLocation, defaultTerm, searchLimit)
                .subscribe(
                        places -> processAndNotify(places, null, notificationTopic),
                        error -> logger.error("Error during weekly gathering spot job: {}", error.getMessage())
                );
    }

    /**
     * Process the place search response and send push notification with random gathering spot
     * @param places List of places from search results
     * @param cityId City ID (null for default configuration)
     * @param topic Firebase topic to send notification to
     */
    private void processAndNotify(List<Place> places, String cityId, String topic) {
        if (places == null || places.isEmpty()) {
            logger.warn("No gathering spots found in search response");
            return;
        }

        logger.info("Retrieved {} potential gathering spots from {}", places.size(),
                placeSearchService.getProviderName());

        // Select a random gathering spot (avoiding recent repeats if cityId is available)
        Place selectedSpot = selectRandomGatheringSpot(places, cityId);

        if (selectedSpot == null) {
            logger.error("Failed to select a gathering spot");
            return;
        }

        logger.info("Selected weekly gathering spot: {} - {} (Rating: {})",
                selectedSpot.getName(),
                selectedSpot.getAddress(),
                selectedSpot.getRating());

        // Save to Firestore if cityId is available
        GatheringSpot gatheringSpot = null;
        if (cityId != null) {
            try {
                gatheringSpot = new GatheringSpot(cityId, selectedSpot);
                gatheringSpotRepository.save(gatheringSpot);
                logger.info("Saved gathering spot to Firestore");
            } catch (Exception e) {
                logger.error("Failed to save gathering spot to Firestore", e);
            }
        }

        // Send push notification
        try {
            pushNotificationService.sendGatheringSpotNotification(selectedSpot, topic);
            logger.info("Successfully sent gathering spot notification for: {}", selectedSpot.getName());

            // Mark notification as sent in Firestore
            if (gatheringSpot != null && gatheringSpot.getId() != null) {
                gatheringSpotRepository.markNotificationSent(gatheringSpot.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to send push notification", e);
        }
    }

    /**
     * Select a random gathering spot from the list, avoiding recent selections
     * @param places List of potential gathering spots to choose from
     * @param cityId City ID (null to skip history check)
     * @return Randomly selected gathering spot
     */
    private Place selectRandomGatheringSpot(List<Place> places, String cityId) {
        if (places == null || places.isEmpty()) {
            return null;
        }

        // If cityId is available, filter out recently selected spots
        if (cityId != null) {
            try {
                String provider = placeSearchService.getProviderName();
                List<String> recentPlaceIds = gatheringSpotRepository.findRecentPlaceIds(
                        cityId, provider, avoidRepeatWeeks);
                logger.info("Found {} spots selected in last {} weeks", recentPlaceIds.size(), avoidRepeatWeeks);

                // Filter out recently selected spots
                List<Place> availableSpots = places.stream()
                        .filter(p -> !recentPlaceIds.contains(p.getProviderId()))
                        .collect(Collectors.toList());

                if (!availableSpots.isEmpty()) {
                    places = availableSpots;
                    logger.info("Filtered to {} available spots (excluding recent selections)", places.size());
                } else {
                    logger.warn("All spots have been recently selected, using full list");
                }
            } catch (Exception e) {
                logger.error("Error checking recent spots, proceeding with full list", e);
            }
        }

        // Simple random selection from available spots
        int randomIndex = random.nextInt(places.size());
        return places.get(randomIndex);
    }
}
