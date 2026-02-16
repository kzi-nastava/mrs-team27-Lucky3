package com.team27.lucky3.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initializes Firebase Admin SDK for server-side FCM push notification delivery.
 * <p>
 * The service-account key file is resolved in order:
 * <ol>
 *   <li>Environment variable / property {@code FIREBASE_SERVICE_ACCOUNT_PATH}
 *       — absolute file path (recommended for production)</li>
 *   <li>Classpath resource {@code firebase-service-account.json}
 *       — convenient for development</li>
 * </ol>
 * If neither source is present, Firebase is <b>not</b> initialized and FCM
 * pushes are silently skipped (the app keeps running without push).
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${FIREBASE_SERVICE_ACCOUNT_PATH:}")
    private String serviceAccountPath;

    @PostConstruct
    public void initFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("FirebaseApp already initialized — skipping");
            return;
        }

        try {
            InputStream serviceAccount = resolveServiceAccount();
            if (serviceAccount == null) {
                log.warn("No Firebase service-account file found — FCM push notifications disabled. "
                        + "Place firebase-service-account.json on classpath or set FIREBASE_SERVICE_ACCOUNT_PATH.");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
            log.info("Firebase Admin SDK initialized successfully — FCM push enabled");
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
        }
    }

    /**
     * Resolves the service-account JSON in priority order:
     * 1. Absolute path from property/env var
     * 2. Classpath resource
     */
    private InputStream resolveServiceAccount() throws IOException {
        // 1. Try explicit file path
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            log.info("Loading Firebase credentials from path: {}", serviceAccountPath);
            return new FileInputStream(serviceAccountPath);
        }

        // 2. Try classpath
        ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
        if (resource.exists()) {
            log.info("Loading Firebase credentials from classpath: firebase-service-account.json");
            return resource.getInputStream();
        }

        return null;
    }
}
