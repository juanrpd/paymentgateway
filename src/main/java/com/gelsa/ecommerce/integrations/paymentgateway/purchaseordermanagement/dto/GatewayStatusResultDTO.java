package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;

public record GatewayStatusResultDTO(
        String providerPaymentId,
        PaymentAttemptStatus status,
        String failureReason
) {
}
