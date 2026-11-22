package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.OrderEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAuditLogEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentWebhookEventEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.OrderRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAttemptRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAuditLogRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentWebhookEventRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.OrderStatus;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentTransitionServiceImpl implements PaymentTransitionService {
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final PaymentAuditLogRepository paymentAuditLogRepository;

    public PaymentTransitionServiceImpl(PaymentWebhookEventRepository webhookEventRepository, PaymentAttemptRepository paymentAttemptRepository, OrderRepository orderRepository, PaymentAuditLogRepository paymentAuditLogRepository) {
        this.webhookEventRepository = webhookEventRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.orderRepository = orderRepository;
        this.paymentAuditLogRepository = paymentAuditLogRepository;
    }

    @Override
    @Transactional
    public void processWebhookEvent(UUID webhookEventId, PaymentAttemptStatus incomingStatus, String failureReason) {
        PaymentWebhookEventEntity event = webhookEventRepository.findById(webhookEventId).orElseThrow(() -> new EntityNotFoundException("Evento de webhook no encontrado: " + webhookEventId));
        PaymentAttemptEntity attempt = paymentAttemptRepository.findByProviderPaymentId(event.getProviderPaymentId()).orElseThrow(() -> new EntityNotFoundException("Intento no encontrado para providerPaymentId: " + event.getProviderPaymentId()));
        OrderEntity order = orderRepository.findByIdForUpdate(attempt.getOrder().getId()).orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + attempt.getOrder().getId()));
        applyStatusTransition(order, attempt, incomingStatus, failureReason, "WEBHOOK_SQS", event);
    }

    @Override
    @Transactional
    public void processReconciliationResult(UUID attemptId, PaymentAttemptStatus incomingStatus, String failureReason) {
        PaymentAttemptEntity attempt = paymentAttemptRepository.findByIdForUpdate(attemptId).orElseThrow(() -> new EntityNotFoundException("Intento no encontrado: " + attemptId));
        OrderEntity order = orderRepository.findByIdForUpdate(attempt.getOrder().getId()).orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + attempt.getOrder().getId()));
        attempt.incrementRetryCount();
        applyStatusTransition(order, attempt, incomingStatus, failureReason, "RECONCILIATION_JOB", null);
    }

    private void applyStatusTransition(
            OrderEntity order,
            PaymentAttemptEntity attempt,
            PaymentAttemptStatus incomingStatus,
            String failureReason,
            String actorSource,
            PaymentWebhookEventEntity webhookEvent
    ) {
        PaymentAttemptStatus currentStatus = attempt.getStatus();

        if (!currentStatus.isValidNextStatus(incomingStatus)) {
            String reason = String.format("Transición ignorada (%s -> %s) desde %s", currentStatus, incomingStatus, actorSource);
            if (webhookEvent != null) {
                webhookEvent.markIgnoredOutOfOrder(reason);
            }
            paymentAuditLogRepository.save(
                    PaymentAuditLogEntity.create(
                            order.getId(),
                            attempt.getId(),
                            currentStatus.name(),
                            incomingStatus.name(),
                            actorSource + "_IGNORED",
                            MDC.get("traceId"), null
                    )
            );
            return;
        }

        attempt.transitionTo(incomingStatus, null, null, failureReason);

        if (webhookEvent != null) {
            webhookEvent.markProcessed();
        }

        paymentAuditLogRepository.save(
                PaymentAuditLogEntity.create(
                        order.getId(),
                        attempt.getId(),
                        currentStatus.name(),
                        incomingStatus.name(),
                        actorSource,
                        MDC.get("traceId"),
                        null
                )
        );

        if (incomingStatus == PaymentAttemptStatus.APPROVED && order.getStatus().isValidNextStatus(OrderStatus.PAID)) {
            String prevOrderStatus = order.getStatus().name();
            order.transitionTo(OrderStatus.PAID);
            paymentAuditLogRepository.save(
                    PaymentAuditLogEntity.create(
                            order.getId(),
                            attempt.getId(),
                            prevOrderStatus,
                            OrderStatus.PAID.name(),
                            actorSource, MDC.get("traceId"),
                            null
                    )
            );
        }
    }
}
