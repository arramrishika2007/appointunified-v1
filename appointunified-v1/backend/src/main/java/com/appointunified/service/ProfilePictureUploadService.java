package com.appointunified.service;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfilePictureUploadService {
    
    private final CloudinaryService cloudinaryService;
    
    @Value("${app.cloudinary.cloud-name:}")
    private String cloudName;
    
    /**
     * Generate Cloudinary signed upload configuration for profile pictures
     * Returns configuration for client-side upload
     */
    public ProfilePictureUploadConfig getUploadConfig(String userId) {
        try {
            CloudinaryService.SignedUploadConfig signedConfig = 
                cloudinaryService.createSignedUploadConfig("appointunified/avatars/" + userId);
            
            return new ProfilePictureUploadConfig(
                signedConfig.cloudName(),
                signedConfig.apiKey(),
                signedConfig.timestamp(),
                signedConfig.folder(),
                signedConfig.signature(),
                "approx. 5MB max",
                "jpg, jpeg, png, webp"
            );
        } catch (Exception e) {
            log.error("Error generating upload config for user {}", userId, e);
            return null;
        }
    }
    
    /**
     * Process uploaded image and generate transformed URLs for different sizes
     * This typically happens after Cloudinary upload via webhook
     */
    public ProcessedImages processUploadedImage(String cloudinaryPublicId, String fileName) {
        try {
            if (cloudName == null || cloudName.isBlank()) {
                log.warn("Cloudinary not configured for image processing");
                return null;
            }
            
            // Base URL for all transformations
            String baseUrl = "https://res.cloudinary.com/" + cloudName + "/image/upload/";
            
            // Full size avatar (200x200, centered crop, high quality)
            String avatarUrl = baseUrl + "c_fill,w_200,h_200,q_90/v1/" + 
                URLEncoder.encode(cloudinaryPublicId, StandardCharsets.UTF_8);
            
            // Thumbnail size (40x40, centered crop, optimized)
            String thumbnailUrl = baseUrl + "c_fill,w_40,h_40,q_85/v1/" + 
                URLEncoder.encode(cloudinaryPublicId, StandardCharsets.UTF_8);
            
            log.info("Processed avatar for public_id: {}", cloudinaryPublicId);
            return new ProcessedImages(avatarUrl, thumbnailUrl, cloudinaryPublicId);
            
        } catch (Exception e) {
            log.error("Error processing uploaded image: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * Generate placeholder/default avatar URL
     */
    public String getDefaultAvatarUrl(String initials) {
        try {
            // Using Cloudinary's text overlay to generate avatar with initials
            String encoded = URLEncoder.encode("text:" + initials, StandardCharsets.UTF_8);
            return "https://res.cloudinary.com/" + cloudName + "/image/upload/c_fill,w_200,h_200,bg_auto," +
                   "l_text:arial_60_bold:text:" + initials + ",co_white/bg_gradient.png";
        } catch (Exception e) {
            log.warn("Error generating default avatar URL for initials: {}", initials, e);
            return "/api/avatar/placeholder?initials=" + initials;
        }
    }
    
    // DTOs
    public record ProfilePictureUploadConfig(
        String cloudName,
        String apiKey,
        long timestamp,
        String folder,
        String signature,
        String fileSizeLimit,
        String acceptedFormats
    ) {}
    
    public record ProcessedImages(
        String avatarUrl,
        String thumbnailUrl,
        String cloudinaryPublicId
    ) {}
}
