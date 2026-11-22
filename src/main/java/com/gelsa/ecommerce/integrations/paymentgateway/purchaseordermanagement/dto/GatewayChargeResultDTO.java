package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;

public record GatewayChargeResultDTO(
        PaymentAttemptStatus outcomeStatus,
        String providerPaymentId,
        String redirectUrl,
        String failureReason
) {
}
