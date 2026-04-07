package com.appointunified.controller;

import com.appointunified.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/cloudinary")
public class CloudinaryController {

    private final CloudinaryService cloudinaryService;

    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/signature")
    public ResponseEntity<?> getSignature(
            @RequestParam(defaultValue = "appointunified/verification") String folder) {
        
        CloudinaryService.SignedUploadConfig config = cloudinaryService.createSignedUploadConfig(folder);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", config
        ));
    }
}
