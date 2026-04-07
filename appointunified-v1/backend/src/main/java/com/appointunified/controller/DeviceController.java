package com.appointunified.controller;

import com.appointunified.dto.request.DeviceTokenRequest;
import com.appointunified.dto.response.ApiResponse;
import com.appointunified.entity.User;
import com.appointunified.entity.UserDevice;
import com.appointunified.exception.AppException;
import com.appointunified.repository.UserDeviceRepository;
import com.appointunified.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/devices")
@RequiredArgsConstructor
@Tag(name = "Devices", description = "Save FCM device tokens for push notifications")
public class DeviceController {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;

    @PostMapping("/fcm")
    @Operation(summary = "Register or update the current device FCM token")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody DeviceTokenRequest.Register request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        UserDevice device = userDeviceRepository.findByFcmToken(request.getFcmToken())
                .map(existing -> {
                    existing.setUser(user);
                    existing.setEnabled(true);
                    existing.setPlatform(request.getPlatform());
                    existing.setDeviceName(request.getDeviceName());
                    existing.setLastSeenAt(OffsetDateTime.now());
                    return existing;
                })
                .orElseGet(() -> {
                    UserDevice newDevice = new UserDevice();
                    newDevice.setUser(user);
                    newDevice.setFcmToken(request.getFcmToken());
                    newDevice.setPlatform(request.getPlatform());
                    newDevice.setDeviceName(request.getDeviceName());
                    newDevice.setEnabled(true);
                    newDevice.setLastSeenAt(OffsetDateTime.now());
                    return newDevice;
                });

        userDeviceRepository.save(device);
        return ResponseEntity.ok(ApiResponse.ok("Device registered", null));
    }
}