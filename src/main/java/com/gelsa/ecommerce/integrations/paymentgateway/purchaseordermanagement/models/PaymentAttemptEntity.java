package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.exception.InvalidStateTransitionExceptionDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttemptEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "provider_payment_id", unique = true, length = 128)
    private String providerPaymentId;

    @Column(name = "provider_name", nullable = false, length = 32)
    private String providerName;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PaymentAttemptStatus status;

    @Column(name = "redirect_url", columnDefinition = "TEXT")
    private String redirectUrl;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static PaymentAttemptEntity create(OrderEntity order, String idempotencyKey, String providerName) {
        PaymentAttemptEntity attempt = new PaymentAttemptEntity();
        attempt.id = UUID.randomUUID();
        attempt.order = order;
        attempt.idempotencyKey = idempotencyKey;
        attempt.providerName = providerName;
        attempt.amount = order.getTotalAmount();
        attempt.status = PaymentAttemptStatus.INITIATED;
        attempt.retryCount = BigDecimal.ZERO.intValue();
        attempt.createdAt = OffsetDateTime.now();
        attempt.updatedAt = attempt.createdAt;
        return attempt;
    }

    public void transitionTo(PaymentAttemptStatus newStatus, String providerPaymentId, String redirectUrl, String failureReason) {
        if (!this.status.isValidNextStatus(newStatus)) {
            throw new InvalidStateTransitionExceptionDTO(String.format("Transición inválida en PaymentAttempt %s: %s -> %s", this.id, this.status, newStatus));
        }
        this.status = newStatus;
        if (Objects.nonNull(providerPaymentId) && !providerPaymentId.isBlank()) {
            this.providerPaymentId = providerPaymentId;
        }
        if (Objects.nonNull(redirectUrl)) {
            this.redirectUrl = redirectUrl;
        }
        if (Objects.nonNull(failureReason)) {
            this.failureReason = failureReason;
        }
        this.updatedAt = OffsetDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = OffsetDateTime.now();
    }
}