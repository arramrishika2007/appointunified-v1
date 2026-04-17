package com.appointunified.controller;

import com.appointunified.dto.request.PaymentRequest;
import com.appointunified.dto.response.ApiResponse;
import com.appointunified.dto.response.PaymentResponse;
import com.appointunified.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<PaymentResponse.ConfigStatus>> getPaymentConfig() {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getConfigStatus()));
    }

    @PostMapping("/deposit/create-order")
    public ResponseEntity<ApiResponse<PaymentResponse.OrderDetails>> createDepositOrder(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.CreateOrder request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.createDepositOrder(userId, request)));
    }

    @PostMapping("/balance/create-order")
    public ResponseEntity<ApiResponse<PaymentResponse.OrderDetails>> createBalanceOrder(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.CreateOrder request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.createBalanceOrder(userId, request)));
    }

    @PostMapping("/deposit/verify")
    public ResponseEntity<ApiResponse<PaymentResponse.VerifyResult>> verifyDepositPayment(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.VerifyOrder request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verifyDepositPayment(userId, request)));
    }

    @PostMapping("/balance/verify")
    public ResponseEntity<ApiResponse<PaymentResponse.VerifyResult>> verifyBalancePayment(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.VerifyOrder request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.verifyBalancePayment(userId, request)));
    }

    @PostMapping("/deposit/confirm-checkout")
    public ResponseEntity<ApiResponse<PaymentResponse.VerifyResult>> confirmDepositCheckout(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.ConfirmCheckout request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.confirmDepositCheckout(userId, request)));
    }

    @PostMapping("/balance/confirm-checkout")
    public ResponseEntity<ApiResponse<PaymentResponse.VerifyResult>> confirmBalanceCheckout(
            @AuthenticationPrincipal UUID userId,
            @RequestBody @Valid PaymentRequest.ConfirmCheckout request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.confirmBalanceCheckout(userId, request)));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String razorpaySignature,
            @RequestBody String rawPayload) {
        paymentService.handleWebhook(rawPayload, razorpaySignature);
        
        return ResponseEntity.ok().build();
    }
}
