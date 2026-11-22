package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayChargeResultDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayStatusResultDTO;

import java.math.BigDecimal;
import java.util.UUID;

//TODO INTERFAZ PARA CONSUMIR LA PASARELA DE PAGO
public interface PaymentGatewayClientService {

    GatewayChargeResultDTO createCharge(
            UUID orderId,
            UUID attemptId,
            String idempotencyKey,
            String providerName,
            BigDecimal amount,
            String currency,
            String paymentMethodToken
    );

    GatewayStatusResultDTO queryPaymentStatus(String idempotencyKey, String providerPaymentId);
}
