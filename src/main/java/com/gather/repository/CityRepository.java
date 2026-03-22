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
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching enabled cities", e);
            Thread.currentThread().interrupt();
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
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching city by ID: {}", id, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    public Optional<CityJobConfig> findByName(String name) {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("name", name)
                    .limit(1)
                    .get()
                    .get()
                    .getDocuments();

            if (documents.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(documents.get(0).toObject(CityJobConfig.class));
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching city by name: {}", name, e);
            Thread.currentThread().interrupt();
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
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error saving city: {}", city.getName(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save city", e);
        }
    }

    public void delete(String id) {
        try {
            firestore.collection(COLLECTION_NAME).document(id).delete().get();
            logger.info("Deleted city with ID: {}", id);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error deleting city: {}", id, e);
            Thread.currentThread().interrupt();
        }
    }
}
