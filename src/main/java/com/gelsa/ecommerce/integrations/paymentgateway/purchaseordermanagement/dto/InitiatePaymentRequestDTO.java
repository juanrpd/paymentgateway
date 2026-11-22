package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InitiatePaymentRequestDTO(
        @NotBlank @Size(max = 32) String providerName,
        @Size(max = 128) String paymentMethodToken
) {
}
