package com.appointunified.repository;

import com.appointunified.entity.User;
import com.appointunified.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    List<User> findAllByRole(UserRole role);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
