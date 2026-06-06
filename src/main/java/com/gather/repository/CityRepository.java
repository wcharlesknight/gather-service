package com.gather.repository;

import com.gather.model.domain.CityJobConfig;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Repository
public class CityRepository {
    private static final Logger logger = LoggerFactory.getLogger(CityRepository.class);
    private static final String COLLECTION_NAME = "cities";

    private final Firestore firestore;

    public CityRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<CityJobConfig> findAllEnabled() {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("enabled", true)
                    .get()
                    .get()
                    .getDocuments();

            List<CityJobConfig> cities = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                cities.add(document.toObject(CityJobConfig.class));
            }
            return cities;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching enabled cities", e);
            return new ArrayList<>();
        } catch (ExecutionException e) {
            logger.error("Error fetching enabled cities", e);
            return new ArrayList<>();
        }
    }

    public Optional<CityJobConfig> findById(String id) {
        try {
            CityJobConfig city = firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(CityJobConfig.class);
            return Optional.ofNullable(city);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching city by ID: {}", id, e);
            return Optional.empty();
        } catch (ExecutionException e) {
            logger.error("Error fetching city by ID: {}", id, e);
            return Optional.empty();
        }
    }

    public CityJobConfig save(CityJobConfig city) {
        try {
            if (city.getId() == null) {
                city.setCreatedAt(System.currentTimeMillis());
                firestore.collection(COLLECTION_NAME).add(city).get();
            } else {
                firestore.collection(COLLECTION_NAME).document(city.getId()).set(city).get();
            }
            logger.info("Saved city: {}", city.getName());
            return city;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while saving city: {}", city.getName(), e);
            throw new RuntimeException("Failed to save city", e);
        } catch (ExecutionException e) {
            logger.error("Error saving city: {}", city.getName(), e);
            throw new RuntimeException("Failed to save city", e);
        }
    }

    public void delete(String id) {
        try {
            firestore.collection(COLLECTION_NAME).document(id).delete().get();
            logger.info("Deleted city with ID: {}", id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while deleting city: {}", id, e);
        } catch (ExecutionException e) {
            logger.error("Error deleting city: {}", id, e);
        }
    }
}
