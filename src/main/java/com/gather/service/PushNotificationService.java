package com.gather.service;

import com.gather.model.domain.Place;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    /**
     * Send push notification about the weekly gathering spot
     *
     * This method sends a single notification to Firebase Cloud Messaging (FCM) with
     * platform-specific configurations. FCM automatically routes the notification to
     * iOS, Android, and web clients using the appropriate config for each platform.
     *
     * @param place The randomly selected gathering spot (provider-agnostic)
     * @param topic The FCM topic to send to (e.g., "weekly-gather")
     */
    public void sendGatheringSpotNotification(Place place, String topic) {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Skipping push notification.");
            return;
        }

        try {
            Message message = buildMessageBuilder(place).setTopic(topic).build();
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent topic push notification: {}", response);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send topic push notification", e);
            throw new RuntimeException("Failed to send push notification", e);
        } catch (IllegalStateException e) {
            logger.error("Firebase not initialized. Cannot send push notification.", e);
        }
    }

    public void sendToToken(Place place, String fcmToken) {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Skipping push notification.");
            return;
        }

        try {
            Message message = buildMessageBuilder(place).setToken(fcmToken).build();
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent per-user push notification: {}", response);
        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send per-user push notification", e);
        } catch (IllegalStateException e) {
            logger.error("Firebase not initialized. Cannot send push notification.", e);
        }
    }

    private Message.Builder buildMessageBuilder(Place place) {
        String title = "📍 This Week's Gather Spot!";
        String body = String.format("Meet Friday at %s - %s", place.getName(), place.getAddress());

        // FCM rejects null values in the data map, so coalesce every entry to a non-null string.
        Map<String, String> data = new HashMap<>();
        data.put("placeId", nullToEmpty(place.getProviderId()));
        data.put("provider", nullToEmpty(place.getProvider()));
        data.put("businessName", nullToEmpty(place.getName()));
        data.put("rating", place.getRating() != null ? String.valueOf(place.getRating()) : "");
        data.put("address", nullToEmpty(place.getAddress()));
        data.put("url", nullToEmpty(place.getUrl()));
        if (place.getLatitude() != null && place.getLongitude() != null) {
            data.put("latitude", String.valueOf(place.getLatitude()));
            data.put("longitude", String.valueOf(place.getLongitude()));
        }

        return Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .build())
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .setChannelId("weekly_recommendations")
                                .build())
                        .build());
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
