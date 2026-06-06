package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.model.dto.request.RegisterRequest;
import com.gather.model.dto.response.AuthResponse;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;

    public AuthService(FirebaseAuth firebaseAuth, Firestore firestore) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
    }

    public AuthResponse register(RegisterRequest request) {
        String displayName = request.getDisplayName().trim();
        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.getEmail())
                    .setPassword(request.getPassword())
                    .setDisplayName(displayName);

            UserRecord userRecord = firebaseAuth.createUser(createRequest);
            String uid = userRecord.getUid();

            try {
                Map<String, Object> userDoc = new HashMap<>();
                userDoc.put("displayName", displayName);
                userDoc.put("email", request.getEmail());
                userDoc.put("createdAt", FieldValue.serverTimestamp());
                userDoc.put("lastLoginAt", FieldValue.serverTimestamp());
                userDoc.put("hasCompletedOnboarding", false);
                firestore.collection("users").document(uid).set(userDoc).get();
            } catch (InterruptedException | ExecutionException e) {
                // The Auth user exists but the profile write failed. Roll back the Auth user so
                // the email is freed and the client can retry, instead of being stuck at 409.
                rollbackAuthUser(uid);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                logger.error("Firestore error during registration; rolled back auth user {}", uid, e);
                throw new RuntimeException("Failed to create user profile", e);
            }

            String customToken = firebaseAuth.createCustomToken(uid);

            logger.info("User registered successfully: {}", uid);
            return AuthResponse.builder()
                    .customToken(customToken)
                    .uid(uid)
                    .displayName(displayName)
                    .email(request.getEmail())
                    .build();

        } catch (FirebaseAuthException e) {
            if (e.getAuthErrorCode() == AuthErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new EmailAlreadyExistsException();
            }
            logger.error("Firebase Auth error during registration", e);
            throw new RuntimeException("Registration failed", e);
        }
    }

    private void rollbackAuthUser(String uid) {
        try {
            firebaseAuth.deleteUser(uid);
            logger.warn("Rolled back orphaned auth user {} after failed profile write", uid);
        } catch (FirebaseAuthException deleteEx) {
            logger.error("Failed to roll back orphaned auth user {}; manual cleanup required", uid, deleteEx);
        }
    }

    public AuthResponse login(String idToken) {
        try {
            FirebaseToken decodedToken = firebaseAuth.verifyIdToken(idToken, true);
            String uid = decodedToken.getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastLoginAt", FieldValue.serverTimestamp());
            firestore.collection("users").document(uid).update(updates).get();

            logger.info("User login recorded: {}", uid);
            return AuthResponse.builder()
                    .uid(uid)
                    .displayName((String) decodedToken.getClaims().get("name"))
                    .email(decodedToken.getEmail())
                    .build();

        } catch (FirebaseAuthException e) {
            logger.error("Invalid ID token during login", e);
            throw new InvalidTokenException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted during login", e);
            throw new RuntimeException("Failed to update login timestamp", e);
        } catch (ExecutionException e) {
            logger.error("Firestore error during login", e);
            throw new RuntimeException("Failed to update login timestamp", e);
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {}
}
