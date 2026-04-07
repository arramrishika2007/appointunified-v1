package com.appointunified.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class FirebaseAdminConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${FIREBASE_CREDENTIALS:}')")
    public FirebaseApp firebaseApp(
            @Value("${FIREBASE_CREDENTIALS}") String base64Credentials,
            @Value("${FIREBASE_PROJECT_ID:}") String projectId) throws Exception {

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        byte[] decoded = Base64.getDecoder().decode(base64Credentials);
        FirebaseOptions.Builder builder = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(decoded)));

        if (org.springframework.util.StringUtils.hasText(projectId)) {
            builder.setProjectId(projectId);
        }

        FirebaseApp app = FirebaseApp.initializeApp(builder.build());
        log.info("Firebase Admin initialized for project {}", projectId);
        return app;
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${FIREBASE_CREDENTIALS:}')")
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${FIREBASE_CREDENTIALS:}')")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}