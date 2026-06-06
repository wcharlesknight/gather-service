package com.gather.repository;

import com.gather.model.domain.GatheringSpot;
import com.google.cloud.firestore.DocumentReference;
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

    public GatheringSpot save(GatheringSpot spot) {
        try {
            if (spot.getId() == null) {
                // add() returns the reference to the new auto-ID document; capture it so the
                // caller (e.g. markNotificationSent) has the persisted document ID.
                DocumentReference ref = firestore.collection(COLLECTION_NAME).add(spot).get();
                spot.setId(ref.getId());
            } else {
                firestore.collection(COLLECTION_NAME).document(spot.getId()).set(spot).get();
            }
            logger.info("Saved gathering spot: {} for city: {}", spot.getBusinessName(), spot.getCityId());
            return spot;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while saving gathering spot", e);
            throw new RuntimeException("Failed to save gathering spot", e);
        } catch (ExecutionException e) {
            logger.error("Error saving gathering spot", e);
            throw new RuntimeException("Failed to save gathering spot", e);
        }
    }

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching recent gathering spots for city: {}", cityId, e);
            return new ArrayList<>();
        } catch (ExecutionException e) {
            logger.error("Error fetching recent gathering spots for city: {}", cityId, e);
            return new ArrayList<>();
        }
    }

    public List<String> findRecentPlaceIds(String cityId, String provider, int weeksBack) {
        long millisecondsBack = (long) weeksBack * 7 * 24 * 60 * 60 * 1000;
        long cutoffTime = System.currentTimeMillis() - millisecondsBack;

        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("cityId", cityId)
                    .whereEqualTo("provider", provider)
                    .whereGreaterThan("selectedAt", cutoffTime)
                    .get()
                    .get()
                    .getDocuments();

            return documents.stream()
                    .map(doc -> doc.toObject(GatheringSpot.class))
                    .map(spot -> {
                        if ("google".equals(provider)) {
                            return spot.getGooglePlaceId();
                        }
                        return null;
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching recent place IDs for provider {} in city: {}", provider, cityId, e);
            return new ArrayList<>();
        } catch (ExecutionException e) {
            logger.error("Error fetching recent place IDs for provider {} in city: {}", provider, cityId, e);
            return new ArrayList<>();
        }
    }

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching all gathering spots for city: {}", cityId, e);
            return new ArrayList<>();
        } catch (ExecutionException e) {
            logger.error("Error fetching all gathering spots for city: {}", cityId, e);
            return new ArrayList<>();
        }
    }

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
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while updating notification status for spot: {}", spotId, e);
        } catch (ExecutionException e) {
            logger.error("Error updating notification status for spot: {}", spotId, e);
        }
    }
}
