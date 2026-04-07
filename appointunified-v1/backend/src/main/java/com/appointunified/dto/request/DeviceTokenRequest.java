package com.appointunified.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DeviceTokenRequest {

    public static class Register {
        @NotBlank
        private String fcmToken;

        private String platform;
        private String deviceName;

        public String getFcmToken() { return fcmToken; }
        public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getDeviceName() { return deviceName; }
        public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    }
}