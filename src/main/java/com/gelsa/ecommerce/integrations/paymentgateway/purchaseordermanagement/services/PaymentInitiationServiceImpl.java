package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayChargeResultDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.InitiatePaymentRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.PaymentAttemptResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.mappers.PaymentMapper;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.OrderEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAuditLogEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.OrderRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAttemptRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAuditLogRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.OrderStatus;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentInitiationServiceImpl implements PaymentInitiationService {

    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final PaymentGatewayClientService paymentGatewayClient;
    private final PaymentMapper paymentMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentInitiationServiceImpl(OrderRepository orderRepository, PaymentAttemptRepository paymentAttemptRepository, PaymentAuditLogRepository paymentAuditLogRepository, PaymentGatewayClientService paymentGatewayClient, PaymentMapper paymentMapper, TransactionTemplate transactionTemplate) {
        this.orderRepository = orderRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentAuditLogRepository = paymentAuditLogRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.paymentMapper = paymentMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public PaymentAttemptResponseDTO initiatePayment(UUID orderId, String idempotencyKey, InitiatePaymentRequestDTO initiatePaymentRequestDTO) {
        // 1. Idempotencia rápida antes de mutar estado
        Optional<PaymentAttemptEntity> existingAttemptEntity = paymentAttemptRepository.findByIdempotencyKey(idempotencyKey);
        if (existingAttemptEntity.isPresent()) {
            return paymentMapper.toAttemptResponse(existingAttemptEntity.get());
        }

        // 2. Transacción corta: bloquear orden, validar estado, pasar a AWAITING_PAYMENT y crear attempt INITIATED
        PaymentAttemptEntity createdAttempt = transactionTemplate.execute(status -> {
            OrderEntity order = orderRepository.findByIdForUpdate(orderId).orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + orderId));

            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
                throw new IllegalStateException("La orden no admite nuevos intentos de pago en estado: " + order.getStatus());
            }

            String previousOrderStatus = order.getStatus().name();
            if (order.getStatus() == OrderStatus.CREATED) {
                order.transitionTo(OrderStatus.AWAITING_PAYMENT);
                paymentAuditLogRepository.save(
                        PaymentAuditLogEntity.create(
                                order.getId(),
                                null,
                                previousOrderStatus,
                                OrderStatus.AWAITING_PAYMENT.name(),
                                "PAYMENT_INITIATION",
                                MDC.get("traceId"),
                                null
                        )
                );
            }

            PaymentAttemptEntity attempt = PaymentAttemptEntity.create(order, idempotencyKey, initiatePaymentRequestDTO.providerName());
            PaymentAttemptEntity saved = paymentAttemptRepository.saveAndFlush(attempt);

            paymentAuditLogRepository.save(PaymentAuditLogEntity.create(
                    order.getId(), saved.getId(), null, PaymentAttemptStatus.INITIATED.name(),
                    "PAYMENT_INITIATION", MDC.get("traceId"), null
            ));
            return saved;
        });

        // 3. Llamada HTTP a la pasarela FUERA de la transacción de base de datos
        GatewayChargeResultDTO chargeResult = paymentGatewayClient.createCharge(
                orderId,
                createdAttempt.getId(),
                idempotencyKey,
                createdAttempt.getProviderName(),
                createdAttempt.getAmount(),
                createdAttempt.getOrder().getCurrency(),
                initiatePaymentRequestDTO.paymentMethodToken()
        );

        // 4. Transacción corta: persistir resultado inicial del intento (REDIRECT_REQUIRED, REJECTED o UNKNOWN_TIMEOUT)
        PaymentAttemptEntity updatedAttempt = transactionTemplate.execute(status -> {
            PaymentAttemptEntity attempt = paymentAttemptRepository.findByIdForUpdate(createdAttempt.getId())
                    .orElseThrow();
            String previousStatus = attempt.getStatus().name();

            attempt.transitionTo(
                    chargeResult.outcomeStatus(),
                    chargeResult.providerPaymentId(),
                    chargeResult.redirectUrl(),
                    chargeResult.failureReason()
            );

            paymentAuditLogRepository.save(PaymentAuditLogEntity.create(orderId, attempt.getId(), previousStatus, attempt.getStatus().name(),"GATEWAY_SYNC_RESPONSE", MDC.get("traceId"), null));
            return attempt;
        });

        return paymentMapper.toAttemptResponse(updatedAttempt);
    }
}
