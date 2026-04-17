package com.appointunified.repository;

import com.appointunified.entity.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    Optional<PaymentOrder> findByGatewayOrderId(String gatewayOrderId);
    Optional<PaymentOrder> findByAppointmentIdAndPaymentType(UUID appointmentId, String paymentType);
    Optional<PaymentOrder> findByIdAndUserId(UUID id, UUID userId);
}
