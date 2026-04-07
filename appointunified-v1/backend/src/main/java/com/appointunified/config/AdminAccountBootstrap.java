package com.appointunified.config;

import com.appointunified.entity.User;
import com.appointunified.enums.UserRole;
import com.appointunified.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccountBootstrap {

    private static final String DEFAULT_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdminAccounts() {
        upsertAdmin(
                "admin@test.com",
                "+919876543005",
                "Admin User",
                UserRole.ADMIN
        );

        upsertAdmin(
                "superadmin@test.com",
                "+919876543006",
                "Super Admin User",
                UserRole.SUPER_ADMIN
        );
    }

    private void upsertAdmin(String email, String phone, String fullName, UserRole role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> User.builder()
                .email(email)
                .phone(phone)
                .fullName(fullName)
                .role(role)
                .build());

        user.setEmail(email);
        user.setPhone(phone);
        user.setFullName(fullName);
        user.setRole(role);
        user.setVerified(true);
        user.setActive(true);

        String currentHash = user.getPasswordHash();
        if (currentHash == null || !passwordEncoder.matches(DEFAULT_PASSWORD, currentHash)) {
            user.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        }

        userRepository.save(user);
        log.info("Ensured {} account: {}", role, email);
    }
}
