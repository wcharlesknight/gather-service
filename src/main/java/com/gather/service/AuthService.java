package com.gather.service;

import com.gather.exception.InvalidTokenException;
import com.gather.model.dto.request.RegisterRequest;
import com.gather.model.dto.response.AuthResponse;
import com.gather.repository.FirestoreAwait;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
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
                FirestoreAwait.get(firestore.collection("users").document(uid).set(userDoc));
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
            String displayName = (String) decodedToken.getClaims().get("name");
            String email = decodedToken.getEmail();

            // A social sign-in (Google/Apple/Facebook/Twitter) never went through /register, so the
            // user's Firestore profile may not exist yet. update() would fail on a missing doc, so we
            // create-or-merge: first login provisions the profile; subsequent logins just touch
            // lastLoginAt. The doc's prior existence tells us whether this is a brand-new user.
            var docRef = firestore.collection("users").document(uid);
            DocumentSnapshot snapshot = FirestoreAwait.get(docRef.get());
            boolean isNewUser = !snapshot.exists();

            Map<String, Object> updates = new HashMap<>();
            updates.put("lastLoginAt", FieldValue.serverTimestamp());
            if (isNewUser) {
                updates.put("displayName", displayName);
                updates.put("email", email);
                updates.put("provider", signInProvider(decodedToken));
                updates.put("createdAt", FieldValue.serverTimestamp());
                updates.put("hasCompletedOnboarding", false);
            }
            FirestoreAwait.get(docRef.set(updates, SetOptions.merge()));

            logger.info("User login recorded: {} (newUser={})", uid, isNewUser);
            return AuthResponse.builder()
                    .uid(uid)
                    .displayName(displayName)
                    .email(email)
                    .isNewUser(isNewUser)
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

    /**
     * Extracts the sign-in provider (e.g. "password", "google.com", "apple.com") from the token's
     * nested {@code firebase.sign_in_provider} claim. Returns "unknown" if the claim is absent or
     * malformed, so profile creation never fails on a missing provider.
     */
    @SuppressWarnings("unchecked")
    private static String signInProvider(FirebaseToken token) {
        Object firebaseClaim = token.getClaims().get("firebase");
        if (firebaseClaim instanceof Map<?, ?> claims) {
            Object provider = ((Map<String, Object>) claims).get("sign_in_provider");
            if (provider instanceof String s) {
                return s;
            }
        }
        return "unknown";
    }

    public static class EmailAlreadyExistsException extends RuntimeException {}
}
