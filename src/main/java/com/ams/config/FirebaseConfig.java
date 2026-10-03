package com.ams.config;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    @Value("${firebase.project-id:}")
    private String projectId;

    @PostConstruct
    public void initializeFirebase() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            InputStream serviceAccount = null;

            if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
                logger.info("Initializing Firebase Admin SDK from credentials file path.");
                serviceAccount = new FileInputStream(credentialsPath.trim());
            } else if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
                logger.info("Initializing Firebase Admin SDK from environment JSON configuration.");
                serviceAccount = new ByteArrayInputStream(credentialsJson.trim().getBytes(StandardCharsets.UTF_8));
            }

            if (serviceAccount != null) {
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount));

                if (projectId != null && !projectId.trim().isEmpty()) {
                    optionsBuilder.setProjectId(projectId.trim());
                }

                FirebaseApp.initializeApp(optionsBuilder.build());
                logger.info("Firebase Admin SDK successfully initialized.");
            } else {
                logger.info("Firebase Admin SDK: No external credentials configured. Running in unconfigured mode for local foundation.");
            }
        } catch (Exception e) {
            logger.warn("Firebase Admin SDK initialization skipped or encountered error: {}", e.getMessage());
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth() {
        if (FirebaseApp.getApps().isEmpty()) {
            return null;
        }
        return FirebaseAuth.getInstance();
    }
}
