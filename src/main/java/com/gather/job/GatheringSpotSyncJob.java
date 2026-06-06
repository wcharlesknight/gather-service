package com.gather.job;

import com.gather.model.domain.CityJobConfig;
import com.gather.model.domain.GatheringSpot;
import com.gather.model.domain.Place;
import com.gather.model.domain.UserProfile;
import com.gather.service.CityService;
import com.gather.service.EmailService;
import com.gather.service.GatheringSpotService;
import com.gather.service.PlaceSearchService;
import com.gather.service.PushNotificationService;
import com.gather.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

/**
 * Weekly job that selects a gathering spot per city and notifies users.
 * Active provider is selected via configuration (place-service.active-provider).
 *
 * <p>Runs synchronously: each city's place search is blocked on (this executes on the scheduler /
 * admin-trigger thread, not a reactive pipeline), so errors propagate to {@link #selectWeeklyGatheringSpot()}
 * and the method only returns once all work is done.
 */
@Component
public class GatheringSpotSyncJob {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotSyncJob.class);

    private final PlaceSearchService placeSearchService;
    private final PushNotificationService pushNotificationService;
    private final CityService cityService;
    private final GatheringSpotService gatheringSpotService;
    private final UserService userService;
    private final EmailService emailService;
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
                                CityService cityService,
                                GatheringSpotService gatheringSpotService,
                                UserService userService,
                                EmailService emailService) {
        this.placeSearchService = placeSearchService;
        this.pushNotificationService = pushNotificationService;
        this.cityService = cityService;
        this.gatheringSpotService = gatheringSpotService;
        this.userService = userService;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${place-service.job.cron:0 0 9 * * THU}",
            zone = "${place-service.job.timezone:America/Los_Angeles}")
    public void selectWeeklyGatheringSpot() {
        if (!jobEnabled) {
            logger.debug("Weekly gathering spot job is disabled");
            return;
        }

        logger.info("Starting weekly gathering spot selection job using provider: {}",
                placeSearchService.getProviderName());

        try {
            List<CityJobConfig> enabledCities = cityService.getAllEnabled();

            if (!enabledCities.isEmpty()) {
                logger.info("Found {} enabled cities in Firestore", enabledCities.size());
                for (CityJobConfig city : enabledCities) {
                    processCity(city);
                }
            } else {
                logger.info("No cities found in Firestore, using default configuration");
                processDefaultCity();
            }
        } catch (Exception e) {
            logger.error("Exception during weekly gathering spot job", e);
        }
    }

    private void processCity(CityJobConfig city) {
        logger.info("Processing gathering spot for city: {} using {}", city.getName(),
                placeSearchService.getProviderName());

        int limit = city.getSearchLimit() != null ? city.getSearchLimit() : searchLimit;
        List<Place> places = searchPlaces(city.getLocation(), city.getSearchTerm(), limit);
        processAndNotify(places, city.getCityId(), city.getTopic());
    }

    private void processDefaultCity() {
        List<Place> places = searchPlaces(defaultLocation, defaultTerm, searchLimit);
        processAndNotify(places, null, notificationTopic);
    }

    private List<Place> searchPlaces(String location, String term, int limit) {
        return placeSearchService.searchPlaces(location, term, limit)
                .onErrorResume(error -> {
                    logger.error("Place search failed for '{}' in '{}': {}", term, location, error.getMessage());
                    return Mono.just(List.of());
                })
                .blockOptional()
                .orElseGet(List::of);
    }

    private void processAndNotify(List<Place> places, String cityId, String topic) {
        if (places == null || places.isEmpty()) {
            logger.warn("No gathering spots found in search response");
            return;
        }

        logger.info("Retrieved {} potential gathering spots from {}", places.size(),
                placeSearchService.getProviderName());

        Place selectedSpot = selectRandomGatheringSpot(places, cityId);

        if (selectedSpot == null) {
            logger.error("Failed to select a gathering spot");
            return;
        }

        logger.info("Selected weekly gathering spot: {} - {} (Rating: {})",
                selectedSpot.getName(), selectedSpot.getAddress(), selectedSpot.getRating());

        GatheringSpot gatheringSpot = null;
        if (cityId != null) {
            try {
                gatheringSpot = gatheringSpotService.save(new GatheringSpot(cityId, selectedSpot));
                logger.info("Saved gathering spot to Firestore");
            } catch (Exception e) {
                logger.error("Failed to save gathering spot to Firestore", e);
            }
        }

        notifyUsers(selectedSpot, cityId, topic);

        if (gatheringSpot != null && gatheringSpot.getId() != null) {
            gatheringSpotService.markNotificationSent(gatheringSpot.getId());
        }
    }

    private void notifyUsers(Place place, String cityId, String topic) {
        if (cityId == null) {
            logger.warn("cityId is null — city document is missing the 'cityId' field. Falling back to topic broadcast. Add cityId to the Firestore city document.");
            pushNotificationService.sendGatheringSpotNotification(place, topic);
            return;
        }

        List<UserProfile> users = userService.findByCityId(cityId);

        if (users.isEmpty()) {
            logger.info("No users found for city {}, falling back to topic broadcast", cityId);
            pushNotificationService.sendGatheringSpotNotification(place, topic);
            return;
        }

        boolean anyWithoutToken = false;
        for (UserProfile user : users) {
            if (user.getFcmToken() != null && !user.getFcmToken().isBlank()) {
                pushNotificationService.sendToToken(place, user.getFcmToken());
            } else {
                anyWithoutToken = true;
            }

            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                try {
                    emailService.sendGatheringSpotEmail(place, user.getEmail(), user.getDisplayName());
                } catch (Exception e) {
                    logger.error("Failed to send email to a user: {}", e.getMessage());
                }
            }
        }

        if (anyWithoutToken) {
            logger.info("Some users in {} have no FCM token, sending topic broadcast as fallback", cityId);
            pushNotificationService.sendGatheringSpotNotification(place, topic);
        }
    }

    // Package-private for unit testing of the selection + repeat-avoidance logic.
    Place selectRandomGatheringSpot(List<Place> places, String cityId) {
        if (places == null || places.isEmpty()) {
            return null;
        }

        if (cityId != null) {
            try {
                String provider = placeSearchService.getProviderName();
                List<String> recentPlaceIds = gatheringSpotService.getRecentPlaceIds(
                        cityId, provider, avoidRepeatWeeks);
                logger.info("Found {} spots selected in last {} weeks", recentPlaceIds.size(), avoidRepeatWeeks);

                List<Place> availableSpots = places.stream()
                        .filter(p -> !recentPlaceIds.contains(p.getProviderId()))
                        .toList();

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

        return places.get(random.nextInt(places.size()));
    }
}
