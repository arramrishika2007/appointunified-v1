package com.appointunified.service;

import com.appointunified.dto.request.AuthRequest;
import com.appointunified.dto.response.AuthResponse;
import com.appointunified.entity.RefreshToken;
import com.appointunified.entity.User;
import com.appointunified.entity.UserDevice;
import com.appointunified.enums.Sector;
import com.appointunified.enums.UserRole;
import com.appointunified.exception.AppException;
import com.appointunified.repository.UserDeviceRepository;
import com.appointunified.repository.RefreshTokenRepository;
import com.appointunified.repository.UserRepository;
import com.appointunified.security.JwtTokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       UserDeviceRepository userDeviceRepository,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       NotificationService notificationService,
                       ObjectProvider<FirebaseAuth> firebaseAuthProvider) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userDeviceRepository = userDeviceRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.firebaseAuthProvider = firebaseAuthProvider;
    }

    @Transactional
    public AuthResponse.TokenPair signUp(AuthRequest.SignUp request) {
        try {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw AppException.conflict("Phone number already registered");
            }
            if (StringUtils.isNotBlank(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw AppException.conflict("Email already registered");
            }

            UserRole role = UserRole.PUBLIC;
            if (StringUtils.isNotBlank(request.getRole()) && "PROFESSIONAL".equalsIgnoreCase(request.getRole())) {
                role = UserRole.PROFESSIONAL;
            }

            Sector sector = null;
            if (StringUtils.isNotBlank(request.getSector())) {
                try {
                    sector = Sector.valueOf(request.getSector().toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid sector value: {}", request.getSector());
                    sector = null;
                }
            }

            OffsetDateTime now = OffsetDateTime.now();
            User user = new User();
            user.setPhone(request.getPhone());
            user.setEmail(StringUtils.trimToNull(request.getEmail()));
            user.setFullName(request.getFullName());
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setRole(role);
            user.setSector(sector);
            user.setCity(request.getCity());
            user.setAddress(request.getAddress());
            user.setLatitude(request.getLatitude());
            user.setLongitude(request.getLongitude());
            user.setActive(true);
            user.setVerified(false);
            user.setRiskScore(java.math.BigDecimal.ZERO);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);

            user = userRepository.save(user);
            log.info("New user registered: {} ({})", user.getId(), user.getPhone());

            try {
                // Email delivery must not block account creation in local/dev environments.
                notificationService.sendWelcomeEmail(user);
            } catch (Exception ex) {
                log.warn("Welcome email failed for user {}: {}", user.getId(), ex.getMessage());
            }

            return buildTokenPair(user);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error during signup: ", e);
            throw AppException.badRequest("Signup failed: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        User user = resolveUser(request.getIdentifier());

        if (!user.isActive()) {
            throw AppException.forbidden("Account is suspended. Contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.unauthorized("Invalid credentials");
        }

        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair loginWithFirebase(AuthRequest.FirebaseLogin request) {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            throw AppException.badRequest("Firebase auth is not configured on the backend");
        }

        FirebaseToken decoded;
        try {
            decoded = firebaseAuth.verifyIdToken(request.getIdToken());
        } catch (Exception e) {
            throw AppException.unauthorized("Invalid Firebase ID token");
        }

        String email = StringUtils.trimToNull(decoded.getEmail());
        String displayName = StringUtils.trimToNull(decoded.getName());
        if (displayName == null) {
            displayName = "Firebase User";
        }

        User user = null;
        if (email != null) {
            user = userRepository.findByEmail(email).orElse(null);
        }

        boolean isNew = false;
        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setFullName(displayName);
            user.setRole(UserRole.PUBLIC);
            user.setVerified(true);
            user.setActive(true);
            isNew = true;
        } else {
            if (StringUtils.isBlank(user.getFullName()) || user.getFullName().equals(user.getEmail())) {
                user.setFullName(displayName);
            }
            user.setVerified(true);
            user.setActive(true);
        }

        user.setLastLoginAt(OffsetDateTime.now());
        user = userRepository.save(user);

        if (StringUtils.isNotBlank(request.getFcmToken())) {
            upsertDeviceToken(user, request.getFcmToken(), request.getPlatform(), request.getDeviceName());
        }

        if (isNew) {
            try {
                notificationService.sendWelcomeEmail(user);
            } catch (Exception ex) {
                log.warn("Welcome email failed for Firebase user {}: {}", user.getId(), ex.getMessage());
            }
        }

        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair refreshToken(AuthRequest.Refresh request) {
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw AppException.unauthorized("Invalid or expired refresh token");
        }

        String tokenHash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> AppException.unauthorized("Refresh token not found"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw AppException.unauthorized("Refresh token is expired or revoked");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("User {} logged out, all refresh tokens revoked", userId);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private AuthResponse.TokenPair buildTokenPair(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User and User ID cannot be null");
        }
        
        UserRole role = user.getRole();
        if (role == null) {
            role = UserRole.PUBLIC;
            user.setRole(role);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), role.name());
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Store hashed refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawRefreshToken))
                .expiresAt(OffsetDateTime.now().plusSeconds(
                        jwtTokenProvider.getRefreshTokenExpiryMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .role(role.name())
                        .avatarUrl(user.getAvatarUrl())
                        .verified(user.isVerified())
                        .sector(user.getSector() != null ? user.getSector().name() : null)
                        .build())
                .build();
    }

    private User resolveUser(String identifier) {
        // identifier can be phone or email
        if (identifier.startsWith("+") || identifier.matches("\\d+")) {
            return userRepository.findByPhone(identifier)
                    .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));
        }
        return userRepository.findByEmail(identifier)
                .orElseThrow(() -> AppException.unauthorized("Invalid credentials"));
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void upsertDeviceToken(User user, String fcmToken, String platform, String deviceName) {
        UserDevice device = userDeviceRepository.findByFcmToken(fcmToken)
                .orElseGet(UserDevice::new);

        device.setUser(user);
        device.setFcmToken(fcmToken);
        device.setPlatform(platform);
        device.setDeviceName(deviceName);
        device.setEnabled(true);
        device.setLastSeenAt(OffsetDateTime.now());
        userDeviceRepository.save(device);
    }
}
