package com.appointunified.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentResponse {

    public static class ConfigStatus {
        private boolean configured;
        private String provider;
        private String keyId;
        private String reason;

        public boolean isConfigured() { return configured; }
        public void setConfigured(boolean configured) { this.configured = configured; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class OrderDetails {
        private UUID paymentOrderId;
        private String keyId;
        private String provider;
        private String gatewayOrderId;
        private String checkoutUrl;
        private BigDecimal amount;
        private String currency;
        private String status;

        public UUID getPaymentOrderId() { return paymentOrderId; }
        public void setPaymentOrderId(UUID paymentOrderId) { this.paymentOrderId = paymentOrderId; }
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getGatewayOrderId() { return gatewayOrderId; }
        public void setGatewayOrderId(String gatewayOrderId) { this.gatewayOrderId = gatewayOrderId; }
        public String getCheckoutUrl() { return checkoutUrl; }
        public void setCheckoutUrl(String checkoutUrl) { this.checkoutUrl = checkoutUrl; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class VerifyResult {
        private UUID paymentOrderId;
        private String paymentType;
        private String paymentStatus;
        private UUID appointmentId;
        private String appointmentStatus;

        public UUID getPaymentOrderId() { return paymentOrderId; }
        public void setPaymentOrderId(UUID paymentOrderId) { this.paymentOrderId = paymentOrderId; }
        public String getPaymentType() { return paymentType; }
        public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public String getAppointmentStatus() { return appointmentStatus; }
        public void setAppointmentStatus(String appointmentStatus) { this.appointmentStatus = appointmentStatus; }
    }
}
