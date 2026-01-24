package com.gather.repository;

import com.gather.model.City;
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

    /**
     * Get all enabled cities
     */
    public List<City> findAllEnabled() {
        try {
            List<QueryDocumentSnapshot> documents = firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("enabled", true)
                    .get()
                    .get()
                    .getDocuments();

            List<City> cities = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                cities.add(document.toObject(City.class));
            }
            return cities;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching enabled cities", e);
            Thread.currentThread().interrupt();
            return new ArrayList<>();
        }
    }

    /**
     * Get city by ID
     */
    public Optional<City> findById(String id) {
        try {
            City city = firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .get()
                    .get()
                    .toObject(City.class);
            return Optional.ofNullable(city);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching city by ID: {}", id, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Get city by name
     */
    public Optional<City> findByName(String name) {
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
            return Optional.of(documents.get(0).toObject(City.class));
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error fetching city by name: {}", name, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    /**
     * Save or update a city
     */
    public City save(City city) {
        try {
            if (city.getId() == null) {
                // Create new document with auto-generated ID
                city.setCreatedAt(System.currentTimeMillis());
                firestore.collection(COLLECTION_NAME)
                        .add(city)
                        .get();
            } else {
                // Update existing document
                firestore.collection(COLLECTION_NAME)
                        .document(city.getId())
                        .set(city)
                        .get();
            }
            logger.info("Saved city: {}", city.getName());
            return city;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error saving city: {}", city.getName(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to save city", e);
        }
    }

    /**
     * Delete a city
     */
    public void delete(String id) {
        try {
            firestore.collection(COLLECTION_NAME)
                    .document(id)
                    .delete()
                    .get();
            logger.info("Deleted city with ID: {}", id);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error deleting city: {}", id, e);
            Thread.currentThread().interrupt();
        }
    }
}
