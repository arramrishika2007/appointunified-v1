package com.appointunified.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    public static class SignUp {
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Phone must be in E.164 format e.g. +919876543210")
        private String phone;

        private String email;

        @NotBlank
        @Size(min = 2, max = 100)
        private String fullName;

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String role; // defaults to PUBLIC
        private String sector;

        private String city;
        private String address;
        private Double latitude;
        private Double longitude;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getSector() { return sector; }
        public void setSector(String sector) { this.sector = sector; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
    }

    public static class Login {
        @NotBlank
        private String identifier; // phone or email
        @NotBlank
        private String password;

        public String getIdentifier() { return identifier; }
        public void setIdentifier(String identifier) { this.identifier = identifier; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class OtpSend {
        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{6,14}$")
        private String phone;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
    }

    public static class OtpVerify {
        @NotBlank
        private String phone;
        @NotBlank
        @Size(min = 6, max = 6)
        private String otp;
        // Firebase ID token from client-side verification
        private String firebaseIdToken;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
        public String getFirebaseIdToken() { return firebaseIdToken; }
        public void setFirebaseIdToken(String firebaseIdToken) { this.firebaseIdToken = firebaseIdToken; }
    }

    public static class Refresh {
        @NotBlank
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class FirebaseLogin {
        @NotBlank
        private String idToken;

        private String fcmToken;
        private String platform;
        private String deviceName;

        public String getIdToken() { return idToken; }
        public void setIdToken(String idToken) { this.idToken = idToken; }
        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
}
