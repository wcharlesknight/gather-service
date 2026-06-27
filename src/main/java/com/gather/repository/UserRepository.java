package com.gather.repository;

import com.gather.model.domain.UserProfile;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Repository
public class UserRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);
    private static final String COLLECTION_NAME = "users";

    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public List<UserProfile> findByCityId(String cityId) {
        try {
            List<QueryDocumentSnapshot> documents = FirestoreAwait.get(firestore.collection(COLLECTION_NAME)
                    .whereEqualTo("location.cityId", cityId)
                    .get())
                    .getDocuments();

            List<UserProfile> users = new ArrayList<>();
            for (QueryDocumentSnapshot document : documents) {
                users.add(document.toObject(UserProfile.class));
            }
            logger.info("Found {} users in city: {}", users.size(), cityId);
            return users;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while fetching users for city: {}", cityId, e);
            return new ArrayList<>();
        } catch (ExecutionException e) {
            logger.error("Error fetching users for city: {}", cityId, e);
            return new ArrayList<>();
        }
    }
}
