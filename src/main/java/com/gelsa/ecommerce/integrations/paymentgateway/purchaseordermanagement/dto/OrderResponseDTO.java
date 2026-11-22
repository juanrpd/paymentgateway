package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponseDTO(
        UUID orderId,
        String customerId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        List<PaymentAttemptResponseDTO> attempts,
        OffsetDateTime createdAt
) {
}
