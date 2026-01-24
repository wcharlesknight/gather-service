package com.gather.job;

import com.gather.model.City;
import com.gather.model.GatheringSpot;
import com.gather.model.YelpBusiness;
import com.gather.model.YelpSearchResponse;
import com.gather.repository.CityRepository;
import com.gather.repository.GatheringSpotRepository;
import com.gather.service.PushNotificationService;
import com.gather.service.YelpApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class YelpDataSyncJob {
    private static final Logger logger = LoggerFactory.getLogger(YelpDataSyncJob.class);

    private final YelpApiService yelpApiService;
    private final PushNotificationService pushNotificationService;
    private final CityRepository cityRepository;
    private final GatheringSpotRepository gatheringSpotRepository;
    private final Random random = new Random();

    @Value("${yelp.job.enabled:true}")
    private boolean jobEnabled;

    @Value("${yelp.job.default-location:San Francisco, CA}")
    private String defaultLocation;

    @Value("${yelp.job.default-term:restaurants}")
    private String defaultTerm;

    @Value("${yelp.job.search-limit:50}")
    private int searchLimit;

    @Value("${firebase.notification-topic:weekly-gather}")
    private String notificationTopic;

    @Value("${yelp.job.avoid-repeat-weeks:12}")
    private int avoidRepeatWeeks;

    public YelpDataSyncJob(YelpApiService yelpApiService,
                          PushNotificationService pushNotificationService,
                          CityRepository cityRepository,
                          GatheringSpotRepository gatheringSpotRepository) {
        this.yelpApiService = yelpApiService;
        this.pushNotificationService = pushNotificationService;
        this.cityRepository = cityRepository;
        this.gatheringSpotRepository = gatheringSpotRepository;
    }

    /**
     * Scheduled job that runs weekly to select a gathering spot and notify users
     * Default: Every Thursday at 9:00 AM (notification sent Thursday for Friday gathering)
     * Cron format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "${yelp.job.cron:0 0 9 * * THU}")
    public void selectWeeklyGatheringSpot() {
        if (!jobEnabled) {
            logger.debug("Weekly gathering spot job is disabled");
            return;
        }

        logger.info("Starting weekly gathering spot selection job");

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
        logger.info("Processing gathering spot for city: {}", city.getName());

        yelpApiService.searchBusinesses(city.getLocation(), city.getSearchTerm(), city.getSearchLimit())
                .subscribe(
                        response -> processAndNotify(response, city.getId(), city.getTopic()),
                        error -> logger.error("Error during gathering spot job for {}: {}",
                                city.getName(), error.getMessage())
                );
    }

    /**
     * Process with default configuration (fallback)
     */
    private void processDefaultCity() {
        yelpApiService.searchBusinesses(defaultLocation, defaultTerm, searchLimit)
                .subscribe(
                        response -> processAndNotify(response, null, notificationTopic),
                        error -> logger.error("Error during weekly gathering spot job: {}", error.getMessage())
                );
    }

    /**
     * Process the Yelp response and send push notification with random gathering spot
     * @param response Yelp search response
     * @param cityId City ID (null for default configuration)
     * @param topic Firebase topic to send notification to
     */
    private void processAndNotify(YelpSearchResponse response, String cityId, String topic) {
        if (response == null || response.getBusinesses() == null || response.getBusinesses().isEmpty()) {
            logger.warn("No gathering spots found in Yelp response");
            return;
        }

        List<YelpBusiness> businesses = response.getBusinesses();
        logger.info("Retrieved {} potential gathering spots from Yelp", businesses.size());

        // Select a random gathering spot (avoiding recent repeats if cityId is available)
        YelpBusiness selectedSpot = selectRandomGatheringSpot(businesses, cityId);

        if (selectedSpot == null) {
            logger.error("Failed to select a gathering spot");
            return;
        }

        logger.info("Selected weekly gathering spot: {} - {} (Rating: {})",
                selectedSpot.getName(),
                selectedSpot.getLocation().getFormattedAddress(),
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
     * @param businesses List of potential gathering spots to choose from
     * @param cityId City ID (null to skip history check)
     * @return Randomly selected gathering spot
     */
    private YelpBusiness selectRandomGatheringSpot(List<YelpBusiness> businesses, String cityId) {
        if (businesses == null || businesses.isEmpty()) {
            return null;
        }

        // If cityId is available, filter out recently selected spots
        if (cityId != null) {
            try {
                List<String> recentYelpIds = gatheringSpotRepository.findRecentYelpIds(cityId, avoidRepeatWeeks);
                logger.info("Found {} spots selected in last {} weeks", recentYelpIds.size(), avoidRepeatWeeks);

                // Filter out recently selected spots
                List<YelpBusiness> availableSpots = businesses.stream()
                        .filter(b -> !recentYelpIds.contains(b.getId()))
                        .collect(Collectors.toList());

                if (!availableSpots.isEmpty()) {
                    businesses = availableSpots;
                    logger.info("Filtered to {} available spots (excluding recent selections)", businesses.size());
                } else {
                    logger.warn("All spots have been recently selected, using full list");
                }
            } catch (Exception e) {
                logger.error("Error checking recent spots, proceeding with full list", e);
            }
        }

        // Simple random selection from available spots
        int randomIndex = random.nextInt(businesses.size());
        return businesses.get(randomIndex);

        // Future enhancement: Weighted selection based on rating or other criteria
        // Could favor higher-rated spots or spots with specific attributes
    }
}

