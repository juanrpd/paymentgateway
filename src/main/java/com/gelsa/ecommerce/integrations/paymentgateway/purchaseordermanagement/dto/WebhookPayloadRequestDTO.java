package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookPayloadRequestDTO(
        @NotBlank String eventId,
        @NotBlank String paymentId,
        @NotBlank String eventType,
        @NotNull PaymentAttemptStatus status,
        String failureReason
) {
}
