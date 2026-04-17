package com.appointunified.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CloudinaryService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryService.class);

    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${CLOUDINARY_URL:}")
    private String cloudinaryUrl;

    @PostConstruct
    void initFromCloudinaryUrl() {
        // Backward-compatible fallback for env files that only provide CLOUDINARY_URL.
        if ((isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) && !isBlank(cloudinaryUrl)) {
            try {
                URI uri = URI.create(cloudinaryUrl);
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();

                if (!isBlank(userInfo)) {
                    String[] creds = userInfo.split(":", 2);
                    if (creds.length == 2) {
                        if (isBlank(apiKey)) {
                            apiKey = creds[0];
                        }
                        if (isBlank(apiSecret)) {
                            apiSecret = creds[1];
                        }
                    }
                }

                if (isBlank(cloudName) && !isBlank(host)) {
                    cloudName = host;
                }
            } catch (Exception ex) {
                log.warn("Failed to parse CLOUDINARY_URL. Falling back to explicit Cloudinary fields only.");
            }
        }
    }

    public record SignedUploadConfig(String cloudName, String apiKey, long timestamp, String folder, String signature) {}

    public SignedUploadConfig createSignedUploadConfig(String folder) {
        if (cloudName == null || cloudName.isBlank() || apiKey == null || apiKey.isBlank() || apiSecret == null || apiSecret.isBlank()) {
            throw new IllegalStateException("Cloudinary is not configured properly");
        }

        long timestamp = Instant.now().getEpochSecond();

        Map<String, String> paramsToSign = new LinkedHashMap<>();
        paramsToSign.put("folder", folder);
        paramsToSign.put("timestamp", String.valueOf(timestamp));

        String signatureBase = paramsToSign.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .reduce((left, right) -> left + "&" + right)
                .orElse("");

        String signature = sha1Hex(signatureBase + apiSecret);
        return new SignedUploadConfig(cloudName, apiKey, timestamp, folder, signature);
    }

    public String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private String sha1Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 algorithm unavailable", e);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
