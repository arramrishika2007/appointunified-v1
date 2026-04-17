package com.appointunified.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.User;
import com.appointunified.repository.UserRepository;
import com.appointunified.service.ProfilePictureUploadService;
import com.appointunified.service.ProfilePictureUploadService.ProcessedImages;
import com.appointunified.service.ProfilePictureUploadService.ProfilePictureUploadConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/profile-picture")
@Slf4j
@RequiredArgsConstructor
public class ProfilePictureController {
    
    private final ProfilePictureUploadService uploadService;
    private final UserRepository userRepository;
    
    /**
     * Get upload configuration for profile picture
     * GET /api/profile-picture/upload-config
     */
    @GetMapping("/upload-config")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL')")
    public ResponseEntity<ApiResponse<ProfilePictureUploadConfig>> getUploadConfig(
            @AuthenticationPrincipal UUID userId) {
        try {
            User currentUser = userRepository.findById(userId)
                .orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
            }
            
            ProfilePictureUploadConfig config = uploadService.getUploadConfig(currentUser.getId().toString());
            if (config == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Profile picture upload is not configured"));
            }
            log.info("Generated upload config for user {}", currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.ok(config));
            
        } catch (Exception e) {
            log.error("Error generating upload config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate upload config"));
        }
    }
    
    /**
     * Process uploaded image (typically called after Cloudinary upload via webhook)
     * POST /api/profile-picture/process
     */
    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL')")
    public ResponseEntity<ApiResponse<ProcessedAvatarResponse>> processUploadedImage(
            @RequestBody ProcessImageRequest request,
            @AuthenticationPrincipal UUID userId) {
        try {
            User currentUser = userRepository.findById(userId)
                .orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
            }
            
            // Validate request
            if (request == null || request.getCloudinaryPublicId() == null || 
                request.getCloudinaryPublicId().isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid cloudinary public ID"));
            }
            
            // Process the image and get transformed URLs
            ProcessedImages processed = uploadService.processUploadedImage(
                request.getCloudinaryPublicId(),
                request.getFileName()
            );
            
            if (processed == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Failed to process image"));
            }
            
            // Update user avatar URLs
            currentUser.setAvatarUrl(processed.avatarUrl());
            currentUser.setAvatarThumbUrl(processed.thumbnailUrl());
            userRepository.save(currentUser);
            
            log.info("Avatar updated for user {}", currentUser.getId());
            
            ProcessedAvatarResponse response = new ProcessedAvatarResponse(
                processed.avatarUrl(),
                processed.thumbnailUrl(),
                processed.cloudinaryPublicId()
            );
            
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (Exception e) {
            log.error("Error processing uploaded image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to process image"));
        }
    }
    
    /**
     * Delete avatar (revert to default)
     * DELETE /api/profile-picture
     */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('USER', 'PROFESSIONAL')")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(@AuthenticationPrincipal UUID userId) {
        try {
            User currentUser = userRepository.findById(userId)
                .orElse(null);
            
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("User not found"));
            }
            
            // Clear avatar URLs
            currentUser.setAvatarUrl(null);
            currentUser.setAvatarThumbUrl(null);
            userRepository.save(currentUser);
            
            log.info("Avatar deleted for user {}", currentUser.getId());
            
            return ResponseEntity.ok(ApiResponse.ok(null));
            
        } catch (Exception e) {
            log.error("Error deleting avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete avatar"));
        }
    }
    
    // DTOs
    public static class ProcessImageRequest {
        private String cloudinaryPublicId;
        private String fileName;
        
        public String getCloudinaryPublicId() { return cloudinaryPublicId; }
        public void setCloudinaryPublicId(String cloudinaryPublicId) { this.cloudinaryPublicId = cloudinaryPublicId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
    }
    
    public record ProcessedAvatarResponse(
        String avatarUrl,
        String avatarThumbUrl,
        String cloudinaryPublicId
    ) {}
}
