package com.appointunified.config;

import com.appointunified.enums.UserRole;
import jakarta.persistence.Converter;

@Converter
public class UserRoleConverter extends PostgresEnumType<UserRole> {
    public UserRoleConverter() {
        super(UserRole.class);
    }
}
