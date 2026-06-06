package com.gather.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class FirebaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials-path}")
    private Resource credentialsResource;

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Bean
    public FirebaseApp initializeFirebase() {
        if (!firebaseEnabled) {
            logger.warn("Firebase is disabled. Push notifications and Firestore will not be available.");
            return null;
        }

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentialsResource.getInputStream()))
                        .build();

                FirebaseApp app = FirebaseApp.initializeApp(options);
                logger.info("Firebase initialized successfully");
                return app;
            } else {
                logger.info("Firebase already initialized");
                return FirebaseApp.getInstance();
            }
        } catch (IOException e) {
            // Firebase is enabled but its credentials could not be loaded. Fail fast at startup
            // rather than registering null beans that NPE on first use at request time.
            throw new IllegalStateException(
                    "Failed to initialize Firebase from credentials '" + credentialsResource
                            + "'. Set firebase.enabled=false to run without Firebase.", e);
        }
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            logger.warn("Firebase not initialized, Firestore will not be available");
            return null;
        }

        Firestore firestore = FirestoreClient.getFirestore(firebaseApp);
        logger.info("Firestore initialized successfully");
        return firestore;
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            logger.warn("Firebase not initialized, FirebaseAuth will not be available");
            return null;
        }
        return FirebaseAuth.getInstance(firebaseApp);
    }
}

