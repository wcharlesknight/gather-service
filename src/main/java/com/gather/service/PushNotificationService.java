package com.gather.service;

import com.gather.model.YelpBusiness;
import com.google.firebase.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    /**
     * Send push notification about the weekly gathering spot
     * @param business The randomly selected gathering spot from Yelp
     * @param topic The FCM topic to send to (e.g., "weekly-gather")
     */
    public void sendGatheringSpotNotification(YelpBusiness business, String topic) {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Skipping push notification.");
            return;
        }

        try {
            String title = "📍 This Week's Gather Spot!";
            String body = String.format("Meet Friday at %s - %s", business.getName(),
                    business.getLocation().getFormattedAddress());

            Map<String, String> data = new HashMap<>();
            data.put("businessId", business.getId());
            data.put("businessName", business.getName());
            data.put("rating", String.valueOf(business.getRating()));
            data.put("address", business.getLocation().getFormattedAddress());
            data.put("url", business.getUrl());
            if (business.getCoordinates() != null) {
                data.put("latitude", String.valueOf(business.getCoordinates().getLatitude()));
                data.put("longitude", String.valueOf(business.getCoordinates().getLongitude()));
            }

            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .setTopic(topic)
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
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent push notification: {}", response);

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send push notification", e);
            throw new RuntimeException("Failed to send push notification", e);
        } catch (IllegalStateException e) {
            logger.error("Firebase not initialized. Cannot send push notification.", e);
        }
    }

    /**
     * Send push notification to specific device tokens
     * @param business The randomly selected gathering spot
     * @param deviceTokens List of FCM device tokens
     */
    public void sendGatheringSpotNotificationToDevices(YelpBusiness business, List<String> deviceTokens) {
        if (!firebaseEnabled || deviceTokens == null || deviceTokens.isEmpty()) {
            logger.warn("Firebase disabled or no device tokens provided. Skipping notification.");
            return;
        }

        try {
            String title = "📍 This Week's Gather Spot!";
            String body = String.format("Meet Friday at %s - %s", business.getName(),
                    business.getLocation().getFormattedAddress());

            Map<String, String> data = new HashMap<>();
            data.put("businessId", business.getId());
            data.put("businessName", business.getName());
            data.put("rating", String.valueOf(business.getRating()));
            data.put("address", business.getLocation().getFormattedAddress());
            data.put("url", business.getUrl());

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putAllData(data)
                    .addAllTokens(deviceTokens)
                    .build();

            BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(message);
            logger.info("Successfully sent {} notifications, {} failures",
                    response.getSuccessCount(), response.getFailureCount());

        } catch (FirebaseMessagingException e) {
            logger.error("Failed to send multicast push notification", e);
        }
    }
}
