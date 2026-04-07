package com.appointunified.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private long expiresIn; // seconds
        private UserInfo user;

        public static Builder builder() { return new Builder(); }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(long expiresIn) { this.expiresIn = expiresIn; }
        public UserInfo getUser() { return user; }
        public void setUser(UserInfo user) { this.user = user; }

        public static final class Builder {
            private String accessToken;
            private String refreshToken;
            private String tokenType;
            private long expiresIn;
            private UserInfo user;

            public Builder accessToken(String accessToken) { this.accessToken = accessToken; return this; }
            public Builder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
            public Builder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
            public Builder expiresIn(long expiresIn) { this.expiresIn = expiresIn; return this; }
            public Builder user(UserInfo user) { this.user = user; return this; }
            public TokenPair build() {
                TokenPair pair = new TokenPair();
                pair.setAccessToken(accessToken);
                pair.setRefreshToken(refreshToken);
                pair.setTokenType(tokenType);
                pair.setExpiresIn(expiresIn);
                pair.setUser(user);
                return pair;
            }
        }
    }

    public static class UserInfo {
        private UUID id;
        private String fullName;
        private String phone;
        private String email;
        private String role;
        private String avatarUrl;
        private boolean verified;
        private String sector;

        public static Builder builder() { return new Builder(); }
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }

        public static final class Builder {
            private UUID id;
            private String fullName;
            private String phone;
            private String email;
            private String role;
            private String avatarUrl;
            private boolean verified;
            private String sector;

            public Builder id(UUID id) { this.id = id; return this; }
            public Builder fullName(String fullName) { this.fullName = fullName; return this; }
            public Builder phone(String phone) { this.phone = phone; return this; }
            public Builder email(String email) { this.email = email; return this; }
            public Builder role(String role) { this.role = role; return this; }
            public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
            public Builder verified(boolean verified) { this.verified = verified; return this; }
            public Builder sector(String sector) { this.sector = sector; return this; }

            public UserInfo build() {
                UserInfo info = new UserInfo();
                info.setId(id);
                info.setFullName(fullName);
                info.setPhone(phone);
                info.setEmail(email);
                info.setRole(role);
                info.setAvatarUrl(avatarUrl);
                info.setVerified(verified);
                info.setSector(sector);
                return info;
            }
        }
    }

    public static class OtpSent {
        private String message;
        private String phone;
        private int expiresInSeconds;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public int getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(int expiresInSeconds) { this.expiresInSeconds = expiresInSeconds; }
    }
}
