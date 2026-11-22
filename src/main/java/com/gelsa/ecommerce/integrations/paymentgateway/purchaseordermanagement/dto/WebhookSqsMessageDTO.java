package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;

public record WebhookSqsMessageDTO(
        String eventId,
        String paymentId,
        PaymentAttemptStatus status
) {
}
