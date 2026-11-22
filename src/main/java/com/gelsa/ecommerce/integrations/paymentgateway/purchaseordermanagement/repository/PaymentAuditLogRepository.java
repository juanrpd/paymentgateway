package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentAuditLogRepository extends JpaRepository<PaymentAuditLogEntity, Long> {

    List<PaymentAuditLogEntity> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
