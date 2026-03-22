package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.exception.UnknownCityException;
import com.gather.model.dto.response.LocationResponse;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;
    private final CityRegistry cityRegistry;

    public UserService(FirebaseAuth firebaseAuth, Firestore firestore, CityRegistry cityRegistry) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.cityRegistry = cityRegistry;
    }

    public LocationResponse updateLocation(String idToken, String cityId) {
        String uid = verifyToken(idToken);

        LocationResponse city = cityRegistry.find(cityId);
        if (city == null) {
            throw new UnknownCityException(cityId);
        }

        try {
            Map<String, Object> location = new HashMap<>();
            location.put("cityId", city.getCityId());
            location.put("cityName", city.getCityName());
            location.put("state", city.getState());
            location.put("country", city.getCountry());
            location.put("latitude", city.getLatitude());
            location.put("longitude", city.getLongitude());
            location.put("savedAt", FieldValue.serverTimestamp());

            Map<String, Object> updates = new HashMap<>();
            updates.put("location", location);
            updates.put("hasCompletedOnboarding", true);

            firestore.collection("users").document(uid).update(updates).get();

            logger.info("Location updated for user {}: {}", uid, cityId);
            return city;
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Firestore error updating location for user {}", uid, e);
            throw new RuntimeException("Failed to save location", e);
        }
    }

    public void ensureProfile(String idToken) {
        String uid = verifyToken(idToken);

        try {
            DocumentSnapshot doc = firestore.collection("users").document(uid).get().get();
            if (doc.exists() && !doc.contains("hasCompletedOnboarding")) {
                firestore.collection("users").document(uid)
                        .update("hasCompletedOnboarding", false)
                        .get();
                logger.info("Backfilled hasCompletedOnboarding for user {}", uid);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Firestore error during profile migration for user {}", uid, e);
            throw new RuntimeException("Failed to ensure user profile", e);
        }
    }

    private String verifyToken(String idToken) {
        try {
            FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);
            return decoded.getUid();
        } catch (FirebaseAuthException e) {
            throw new InvalidTokenException();
        }
    }
}
