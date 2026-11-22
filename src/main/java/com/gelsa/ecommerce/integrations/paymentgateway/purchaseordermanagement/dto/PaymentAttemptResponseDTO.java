package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentAttemptResponseDTO(
        UUID attemptId,
        UUID orderId,
        String idempotencyKey,
        String providerPaymentId,
        String providerName,
        BigDecimal amount,
        PaymentAttemptStatus status,
        String redirectUrl,
        String message,
        OffsetDateTime updatedAt
) {
}
