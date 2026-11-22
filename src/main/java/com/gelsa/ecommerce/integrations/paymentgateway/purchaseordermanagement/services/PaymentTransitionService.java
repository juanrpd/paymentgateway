package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;

import java.util.UUID;

public interface PaymentTransitionService {

    void processWebhookEvent(UUID webhookEventId, PaymentAttemptStatus incomingStatus, String failureReason);

    void processReconciliationResult(UUID attemptId, PaymentAttemptStatus incomingStatus, String failureReason);
}
