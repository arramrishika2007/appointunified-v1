package com.appointunified.controller;

import com.appointunified.dto.request.AuthRequest;
import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.AuthResponse;
import com.appointunified.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signup, login, token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> signUp(
            @Valid @RequestBody AuthRequest.SignUp request) {
        var result = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", result));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with phone/email + password")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> refresh(
            @Valid @RequestBody AuthRequest.Refresh request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }


    @PostMapping("/firebase/login")
    @Operation(summary = "Login or register with a Firebase ID token")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> firebaseLogin(
            @Valid @RequestBody AuthRequest.FirebaseLogin request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.loginWithFirebase(request)));
    }
    @DeleteMapping("/logout")
    @Operation(summary = "Logout and revoke refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UUID userId) {
        authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
