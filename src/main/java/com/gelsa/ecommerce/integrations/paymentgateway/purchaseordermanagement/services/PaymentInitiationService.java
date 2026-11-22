package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.InitiatePaymentRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.PaymentAttemptResponseDTO;

import java.util.UUID;

public interface PaymentInitiationService {
    PaymentAttemptResponseDTO initiatePayment(UUID orderId, String idempotencyKey, InitiatePaymentRequestDTO initiatePaymentRequestDTO);
}
