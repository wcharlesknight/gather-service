package com.gather.repository;

import com.gather.model.GatheringSpot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Repository
public class GatheringSpotRepository {
    private static final Logger logger = LoggerFactory.getLogger(GatheringSpotRepository.class);
    private static final String COLLECTION_NAME = "gatheringSpots";

    private final Firestore firestore;

    public GatheringSpotRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    /**
     * Save a gathering spot
     */
    public GatheringSpot save(GatheringSpot spot) {
        try {
            if (spot.getId() == null) {
                // Create new document with auto-generated ID
                firestore.collection(COLLECTION_NAME)
                        .add(spot)
                        .get();
            } else {
                // Update existing document
                firestore.collection(COLLECTION_NAME)
                        .document(spot.getId())
                        .set(spot)
                        .get();
            }
            logger.info("Saved gathering spot: {} for city: {}", spot.getBusinessName(), spot.getCityId());
            return spot;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error saving gathering spot", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save gathering spot", e);
        }
    }

    /**
     * Get recent gathering spots for a city
     * @param cityId The city ID
     * @param limit Number of recent spots to retrieve
     * @return List of recent gathering spots
     */
    public List<GatheringSpot> findRecentByCityId(String cityId, int limit) {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("cityId", cityId)
                    .orderBy("selectedAt", Query.Direction.DESCENDING)
                    .limit(limit)
                    .get()
                    .get()
                    .getDocuments();

            List<GatheringSpot> spots = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                spots.add(document.toObject(GatheringSpot.class));
            }
            return spots;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching recent gathering spots for city: {}", cityId, e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Get Yelp business IDs of recently selected spots for a city
     * @param cityId The city ID
     * @param weeksBack How many weeks back to check
     * @return List of Yelp business IDs
     */
    public List<String> findRecentYelpIds(String cityId, int weeksBack) {
        long millisecondsBack = (long) weeksBack * 7 * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - millisecondsBack;

        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("cityId", cityId)
                    .whereGreaterThan("selectedAt", cutoffTime)
                    .get()
                    .get()
                    .getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(GatheringSpot.class))
                    .map(GatheringSpot::getYelpBusinessId)
                    .collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching recent Yelp IDs for city: {}", cityId, e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Get all gathering spots for a city
     */
    public List<GatheringSpot> findAllByCityId(String cityId) {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("cityId", cityId)
                    .orderBy("selectedAt", Query.Direction.DESCENDING)
                    .get()
                    .get()
                    .getDocuments();

            List<GatheringSpot> spots = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                spots.add(document.toObject(GatheringSpot.class));
            }
            return spots;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching all gathering spots for city: {}", cityId, e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Update notification sent status
     */
    public void markNotificationSent(String spotId) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(spotId)
                    .update(
                            "notificationSent", true,
                            "notificationSentAt", System.currentTimeMillis()
                    )
                    .get();
            logger.info("Marked notification sent for spot: {}", spotId);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error updating notification status for spot: {}", spotId, e);
            Thread.currentThread().interrupt();
        }
    }
}
