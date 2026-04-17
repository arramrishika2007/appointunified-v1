package com.appointunified.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class PaymentRequest {

    public static class CreateOrder {
        @NotNull
        private UUID appointmentId;

        // Optionally, client can pass their own idempotency key.
        private String idempotencyKey;

        public UUID getAppointmentId() { return appointmentId; }
        public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }
        public String getIdempotencyKey() { return idempotencyKey; }
        public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    }

    public static class VerifyOrder {
        @NotNull
        private UUID paymentOrderId;

        @NotBlank
        private String razorpayOrderId;

        @NotBlank
        private String razorpayPaymentId;

        @NotBlank
        private String razorpaySignature;

        public UUID getPaymentOrderId() { return paymentOrderId; }
        public void setPaymentOrderId(UUID paymentOrderId) { this.paymentOrderId = paymentOrderId; }
        public String getRazorpayOrderId() { return razorpayOrderId; }
        public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }
        public String getRazorpayPaymentId() { return razorpayPaymentId; }
        public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }
        public String getRazorpaySignature() { return razorpaySignature; }
        public void setRazorpaySignature(String razorpaySignature) { this.razorpaySignature = razorpaySignature; }
    }

    public static class ConfirmCheckout {
        @NotNull
        private UUID paymentOrderId;

        @NotBlank
        private String checkoutSessionId;

        public UUID getPaymentOrderId() { return paymentOrderId; }
        public void setPaymentOrderId(UUID paymentOrderId) { this.paymentOrderId = paymentOrderId; }
        public String getCheckoutSessionId() { return checkoutSessionId; }
        public void setCheckoutSessionId(String checkoutSessionId) { this.checkoutSessionId = checkoutSessionId; }
    }
}
