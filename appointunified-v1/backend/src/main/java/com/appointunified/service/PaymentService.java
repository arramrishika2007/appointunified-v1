package com.appointunified.service;

import com.appointunified.dto.request.PaymentRequest;
import com.appointunified.dto.response.PaymentResponse;
import com.appointunified.entity.Appointment;
import com.appointunified.entity.PaymentOrder;
import com.appointunified.enums.AppointmentStatus;
import com.appointunified.enums.PaymentStatus;
import com.appointunified.exception.AppException;
import com.appointunified.repository.AppointmentRepository;
import com.appointunified.repository.PaymentOrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.payment.razorpay.key-id:}")
    private String razorpayKeyId;

    @Value("${app.payment.razorpay.key-secret:}")
    private String razorpayKeySecret;

    @Value("${app.payment.razorpay.webhook-secret:}")
    private String razorpayWebhookSecret;

    @Value("${app.payment.stripe.publishable-key:}")
    private String stripePublishableKey;

    @Value("${app.payment.stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    private static final String RAZORPAY_ORDER_URL = "https://api.razorpay.com/v1/orders";
    private static final String STRIPE_SESSION_URL = "https://api.stripe.com/v1/checkout/sessions";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public PaymentResponse.ConfigStatus getConfigStatus() {
        PaymentResponse.ConfigStatus status = new PaymentResponse.ConfigStatus();
        boolean stripeConfigured = isStripeConfigured();
        boolean razorpayConfigured = isRazorpayConfigured();

        if (stripeConfigured) {
            status.setConfigured(true);
            status.setProvider("STRIPE");
            status.setKeyId(stripePublishableKey);
            status.setReason(null);
        } else if (razorpayConfigured) {
            status.setConfigured(true);
            status.setProvider("RAZORPAY");
            status.setKeyId(razorpayKeyId);
            status.setReason(null);
        } else {
            status.setConfigured(false);
            status.setProvider(null);
            status.setKeyId(null);
            status.setReason("No payment gateway is configured on server");
        }
        return status;
    }

    @Transactional
    public PaymentResponse.OrderDetails createDepositOrder(UUID userId, PaymentRequest.CreateOrder request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getClient().getId().equals(userId)) {
            throw AppException.forbidden("Not your appointment");
        }

        if (!appointment.isVirtual()) {
            throw AppException.badRequest("Deposit payment is only supported for online appointments");
        }

        if (appointment.getDepositStatus() == PaymentStatus.CONFIRMED) {
            throw AppException.badRequest("Deposit already paid for this appointment");
        }

        boolean validStatus = appointment.getStatus() == AppointmentStatus.PENDING_DEPOSIT
                || appointment.getStatus() == AppointmentStatus.SCHEDULED
                || appointment.getStatus() == AppointmentStatus.CONFIRMED
                || appointment.getStatus() == AppointmentStatus.DEPOSIT_PAID;
        if (!validStatus) {
            throw AppException.badRequest("Appointment is not eligible for deposit payment");
        }

        // Idempotency check: see if a deposit order already exists
        return paymentOrderRepository.findByAppointmentIdAndPaymentType(appointment.getId(), "DEPOSIT")
                .map(this::toOrderDetails)
                .orElseGet(() -> {
                    // Create new order. 30% deposit
                    BigDecimal depositAmount = appointment.getTotalAmount().multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);

                    PaymentOrder order = new PaymentOrder();
                    order.setAppointment(appointment);
                    order.setUser(appointment.getClient());
                    order.setAmount(depositAmount);
                    order.setPaymentType("DEPOSIT");
                    order.setStatus("PENDING");

                    order = paymentOrderRepository.save(order);
                    order.setGatewayOrderId(createGatewayOrder(order));
                    order = paymentOrderRepository.save(order);

                    log.info("Created DEPOSIT order {} for appointment {}", order.getId(), appointment.getId());
                    return toOrderDetails(order);
                });
    }

    @Transactional
    public PaymentResponse.OrderDetails createBalanceOrder(UUID userId, PaymentRequest.CreateOrder request) {
        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> AppException.notFound("Appointment not found"));

        if (!appointment.getClient().getId().equals(userId)) {
            throw AppException.forbidden("Not your appointment");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING_BALANCE) {
            throw AppException.badRequest("Appointment does not have a pending balance");
        }

        return paymentOrderRepository.findByAppointmentIdAndPaymentType(appointment.getId(), "BALANCE")
                .map(this::toOrderDetails)
                .orElseGet(() -> {
                    BigDecimal total = appointment.getTotalAmount();
                    // 70% remaining balance
                    BigDecimal balanceAmount = total.multiply(new BigDecimal("0.70")).setScale(2, java.math.RoundingMode.HALF_UP);

                    PaymentOrder order = new PaymentOrder();
                    order.setAppointment(appointment);
                    order.setUser(appointment.getClient());
                    order.setAmount(balanceAmount);
                    order.setPaymentType("BALANCE");
                    order.setStatus("PENDING");

                    order = paymentOrderRepository.save(order);
                    order.setGatewayOrderId(createGatewayOrder(order));
                    order = paymentOrderRepository.save(order);

                    log.info("Created BALANCE order {} for appointment {}", order.getId(), appointment.getId());
                    return toOrderDetails(order);
                });
    }

    @Transactional
    public PaymentResponse.VerifyResult verifyDepositPayment(UUID userId, PaymentRequest.VerifyOrder request) {
        PaymentOrder order = loadAndVerifyPaymentRequest(userId, request, "DEPOSIT");
        Appointment appointment = order.getAppointment();

        markDepositConfirmed(appointment);

        return toVerifyResult(order);
    }

    @Transactional
    public PaymentResponse.VerifyResult verifyBalancePayment(UUID userId, PaymentRequest.VerifyOrder request) {
        PaymentOrder order = loadAndVerifyPaymentRequest(userId, request, "BALANCE");
        Appointment appointment = order.getAppointment();

        appointment.setFinalPaymentStatus(PaymentStatus.CONFIRMED);
        appointment.setStatus(AppointmentStatus.PAID_FULL);
        appointmentRepository.save(appointment);

        return toVerifyResult(order);
    }

    @Transactional
    public PaymentResponse.VerifyResult confirmDepositCheckout(UUID userId, PaymentRequest.ConfirmCheckout request) {
        PaymentOrder order = confirmStripeCheckout(userId, request, "DEPOSIT");
        markDepositConfirmed(order.getAppointment());
        return toVerifyResult(order);
    }

    @Transactional
    public PaymentResponse.VerifyResult confirmBalanceCheckout(UUID userId, PaymentRequest.ConfirmCheckout request) {
        PaymentOrder order = confirmStripeCheckout(userId, request, "BALANCE");
        Appointment appointment = order.getAppointment();
        appointment.setFinalPaymentStatus(PaymentStatus.CONFIRMED);
        appointment.setStatus(AppointmentStatus.PAID_FULL);
        appointmentRepository.save(appointment);
        return toVerifyResult(order);
    }

    @Transactional
    public void handleWebhook(String rawPayload, String signature) {
        log.info("Received payment webhook");
        if (signature == null || signature.isBlank()) {
            log.warn("Webhook signature missing");
        } else if (razorpayWebhookSecret != null && !razorpayWebhookSecret.isBlank()) {
            String expected = hmacSha256(rawPayload, razorpayWebhookSecret);
            if (!Objects.equals(expected, signature)) {
                log.warn("Invalid Razorpay webhook signature");
                return;
            }
        } else {
            log.warn("Webhook secret is not configured; webhook is accepted without signature verification");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventName = root.path("event").asText();
            if (!"payment.captured".equals(eventName)) {
                return;
            }

            JsonNode entity = root.path("payload").path("payment").path("entity");
            String gatewayOrderId = entity.path("order_id").asText(null);
            String gatewayPaymentId = entity.path("id").asText(null);

            if (gatewayOrderId == null || gatewayPaymentId == null) {
                return;
            }

            paymentOrderRepository.findByGatewayOrderId(gatewayOrderId).ifPresent(order -> {
                if (!order.getStatus().equals("PAID")) {
                    order.setStatus("PAID");
                    order.setGatewayPaymentId(gatewayPaymentId);
                    paymentOrderRepository.save(order);

                    Appointment appointment = order.getAppointment();
                    if (order.getPaymentType().equals("DEPOSIT")) {
                        markDepositConfirmed(appointment);
                        log.info("Deposit fullfilled for Appointment {}", appointment.getId());
                    } else if (order.getPaymentType().equals("BALANCE")) {
                        appointment.setFinalPaymentStatus(PaymentStatus.CONFIRMED);
                        appointment.setStatus(AppointmentStatus.PAID_FULL);
                        appointmentRepository.save(appointment);
                        log.info("Balance fullfilled for Appointment {}", appointment.getId());
                    }
                }
            });
        } catch (Exception ex) {
            log.warn("Failed to parse payment webhook payload", ex);
        }
    }

    @Transactional
    public PaymentResponse.OrderDetails refund(UUID paymentOrderId, UUID adminUserId) {
        PaymentOrder order = paymentOrderRepository.findById(paymentOrderId)
            .orElseThrow(() -> AppException.notFound("Payment Order not found"));

        if (!order.getStatus().equals("PAID")) {
             throw AppException.badRequest("Can only refund a PAID order. Current status: " + order.getStatus());
        }

        // Call logic to refund on Razorpay/Stripe mock here
        order.setStatus("REFUNDED");
        paymentOrderRepository.save(order);
        
        Appointment appointment = order.getAppointment();
        if (order.getPaymentType().equals("DEPOSIT")) {
               appointment.setDepositStatus(PaymentStatus.REFUNDED);
        } else if (order.getPaymentType().equals("BALANCE")) {
               appointment.setFinalPaymentStatus(PaymentStatus.REFUNDED);
        }
        appointmentRepository.save(appointment);
        
        log.info("Admin {} refunded payment order {}", adminUserId, paymentOrderId);
        return toOrderDetails(order);
    }

    private PaymentResponse.OrderDetails toOrderDetails(PaymentOrder order) {
        PaymentResponse.OrderDetails details = new PaymentResponse.OrderDetails();
        details.setPaymentOrderId(order.getId());

        boolean stripeOrder = isStripeSession(order.getGatewayOrderId());
        details.setProvider(stripeOrder ? "STRIPE" : "RAZORPAY");
        details.setKeyId(stripeOrder ? stripePublishableKey : razorpayKeyId);
        details.setGatewayOrderId(order.getGatewayOrderId());
        details.setCheckoutUrl(stripeOrder ? fetchStripeCheckoutUrl(order.getGatewayOrderId()) : null);
        details.setAmount(order.getAmount());
        details.setCurrency(order.getCurrency());
        details.setStatus(order.getStatus());
        return details;
    }

    private PaymentOrder loadAndVerifyPaymentRequest(UUID userId, PaymentRequest.VerifyOrder request, String expectedType) {
        PaymentOrder order = paymentOrderRepository.findByIdAndUserId(request.getPaymentOrderId(), userId)
                .orElseThrow(() -> AppException.notFound("Payment order not found"));

        if (!Objects.equals(order.getPaymentType(), expectedType)) {
            throw AppException.badRequest("Payment type mismatch");
        }

        if ("PAID".equals(order.getStatus())) {
            return order;
        }

        if (!Objects.equals(order.getGatewayOrderId(), request.getRazorpayOrderId())) {
            throw AppException.badRequest("Gateway order mismatch");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String expectedSignature = hmacSha256(payload, razorpayKeySecret);
        if (!Objects.equals(expectedSignature, request.getRazorpaySignature())) {
            throw AppException.badRequest("Invalid Razorpay signature");
        }

        order.setStatus("PAID");
        order.setGatewayPaymentId(request.getRazorpayPaymentId());
        return paymentOrderRepository.save(order);
    }

    private String createRazorpayOrder(PaymentOrder order) {
        if (razorpayKeyId == null || razorpayKeyId.isBlank() || razorpayKeySecret == null || razorpayKeySecret.isBlank()) {
            throw AppException.badRequest("Razorpay is not configured on server");
        }

        long amountInPaise = order.getAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).longValue();
        Map<String, Object> notes = new HashMap<>();
        notes.put("payment_order_id", order.getId().toString());
        notes.put("appointment_id", order.getAppointment().getId().toString());
        notes.put("payment_type", order.getPaymentType());

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amountInPaise);
        requestBody.put("currency", order.getCurrency());
        requestBody.put("receipt", "au_" + order.getId().toString().replace("-", ""));
        requestBody.put("notes", notes);

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            String auth = Base64.getEncoder().encodeToString((razorpayKeyId + ":" + razorpayKeySecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(RAZORPAY_ORDER_URL))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Razorpay order creation failed. status={}, body={}", response.statusCode(), response.body());
                throw AppException.badRequest("Unable to create Razorpay order");
            }

            JsonNode root = objectMapper.readTree(response.body());
            String orderId = root.path("id").asText(null);
            if (orderId == null || orderId.isBlank()) {
                throw AppException.badRequest("Invalid Razorpay order response");
            }
            return orderId;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create Razorpay order", ex);
            throw AppException.badRequest("Unable to create Razorpay order");
        }
    }

    private String createGatewayOrder(PaymentOrder order) {
        if (isStripeConfigured()) {
            return createStripeCheckoutSession(order);
        }

        if (isRazorpayConfigured()) {
            return createRazorpayOrder(order);
        }

        throw AppException.badRequest("No payment gateway is configured on server");
    }

    private boolean isRazorpayConfigured() {
        return razorpayKeyId != null && !razorpayKeyId.isBlank()
                && razorpayKeySecret != null && !razorpayKeySecret.isBlank();
    }

    private boolean isStripeConfigured() {
        return stripePublishableKey != null && !stripePublishableKey.isBlank()
                && stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private boolean isStripeSession(String gatewayOrderId) {
        return gatewayOrderId != null && gatewayOrderId.startsWith("cs_");
    }

    private String createStripeCheckoutSession(PaymentOrder order) {
        if (!isStripeConfigured()) {
            throw AppException.badRequest("Stripe is not configured on server");
        }

        long amountInMinor = order.getAmount()
                .multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        String currency = (order.getCurrency() == null || order.getCurrency().isBlank())
                ? "usd"
                : order.getCurrency().toLowerCase();

        String successUrl = frontendUrl + "/booking/confirm?appointmentId=" +
                urlEncode(order.getAppointment().getId().toString()) +
                "&payment_order_id=" + urlEncode(order.getId().toString()) +
                "&stripe_session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = frontendUrl + "/booking/confirm?appointmentId=" +
                urlEncode(order.getAppointment().getId().toString());

        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", successUrl);
        form.put("cancel_url", cancelUrl);
        form.put("line_items[0][quantity]", "1");
        form.put("line_items[0][price_data][currency]", currency);
        form.put("line_items[0][price_data][unit_amount]", String.valueOf(amountInMinor));
        form.put("line_items[0][price_data][product_data][name]", "AppointUnified " + order.getPaymentType() + " Payment");
        form.put("metadata[payment_order_id]", order.getId().toString());
        form.put("metadata[appointment_id]", order.getAppointment().getId().toString());
        form.put("metadata[payment_type]", order.getPaymentType());

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_SESSION_URL))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(buildFormBody(form)))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Stripe session creation failed. status={}, body={}", response.statusCode(), response.body());
                throw AppException.badRequest("Unable to create Stripe checkout session");
            }

            JsonNode node = objectMapper.readTree(response.body());
            String sessionId = node.path("id").asText(null);
            if (sessionId == null || sessionId.isBlank()) {
                throw AppException.badRequest("Invalid Stripe checkout response");
            }

            return sessionId;
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create Stripe checkout session", ex);
            throw AppException.badRequest("Unable to create Stripe checkout session");
        }
    }

    private String fetchStripeCheckoutUrl(String sessionId) {
        try {
            JsonNode sessionNode = fetchStripeSession(sessionId);
            String url = sessionNode.path("url").asText(null);
            return (url == null || url.isBlank()) ? null : url;
        } catch (Exception ex) {
            log.warn("Unable to fetch Stripe checkout URL for session {}", sessionId);
            return null;
        }
    }

    private PaymentOrder confirmStripeCheckout(UUID userId, PaymentRequest.ConfirmCheckout request, String expectedType) {
        PaymentOrder order = paymentOrderRepository.findByIdAndUserId(request.getPaymentOrderId(), userId)
                .orElseThrow(() -> AppException.notFound("Payment order not found"));

        if (!Objects.equals(order.getPaymentType(), expectedType)) {
            throw AppException.badRequest("Payment type mismatch");
        }

        if ("PAID".equals(order.getStatus())) {
            return order;
        }

        if (!isStripeSession(order.getGatewayOrderId())) {
            throw AppException.badRequest("Order is not a Stripe checkout order");
        }

        if (!Objects.equals(order.getGatewayOrderId(), request.getCheckoutSessionId())) {
            throw AppException.badRequest("Checkout session mismatch");
        }

        JsonNode sessionNode = fetchStripeSession(request.getCheckoutSessionId());
        String paymentStatus = sessionNode.path("payment_status").asText("");
        if (!"paid".equalsIgnoreCase(paymentStatus)) {
            throw AppException.badRequest("Payment is not completed yet");
        }

        order.setStatus("PAID");
        String paymentIntent = sessionNode.path("payment_intent").asText(null);
        order.setGatewayPaymentId(paymentIntent);
        return paymentOrderRepository.save(order);
    }

    private JsonNode fetchStripeSession(String sessionId) {
        if (!isStripeConfigured()) {
            throw AppException.badRequest("Stripe is not configured on server");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(STRIPE_SESSION_URL + "/" + urlEncode(sessionId)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + stripeSecretKey)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Stripe session fetch failed. status={}, body={}", response.statusCode(), response.body());
                throw AppException.badRequest("Unable to verify Stripe checkout session");
            }

            return objectMapper.readTree(response.body());
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify Stripe checkout session", ex);
            throw AppException.badRequest("Unable to verify Stripe checkout session");
        }
    }

    private String buildFormBody(Map<String, String> formValues) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : formValues.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey()));
            builder.append('=');
            builder.append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac sha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256.init(secretKey);
            byte[] hash = sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw AppException.badRequest("Unable to verify Razorpay signature");
        }
    }

    private PaymentResponse.VerifyResult toVerifyResult(PaymentOrder order) {
        PaymentResponse.VerifyResult result = new PaymentResponse.VerifyResult();
        result.setPaymentOrderId(order.getId());
        result.setPaymentType(order.getPaymentType());
        result.setPaymentStatus(order.getStatus());
        result.setAppointmentId(order.getAppointment().getId());
        result.setAppointmentStatus(order.getAppointment().getStatus().name());
        return result;
    }

    private void markDepositConfirmed(Appointment appointment) {
        appointment.setDepositStatus(PaymentStatus.CONFIRMED);

        if (appointment.getStatus() == AppointmentStatus.PENDING_DEPOSIT
                || appointment.getStatus() == AppointmentStatus.DEPOSIT_PAID
                || appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.SCHEDULED);
        }

        appointmentRepository.save(appointment);
    }
}
