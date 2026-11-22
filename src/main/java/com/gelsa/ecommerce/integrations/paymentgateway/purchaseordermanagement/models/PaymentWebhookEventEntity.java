package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.Constants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookEventEntity {

    @Id
    private UUID id;

    @Column(name = "provider_event_id", nullable = false, unique = true, length = 128)
    private String providerEventId;

    @Column(name = "provider_payment_id", nullable = false, length = 128)
    private String providerPaymentId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "processing_status", nullable = false, length = 32)
    private String processingStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public static PaymentWebhookEventEntity create(String providerEventId, String providerPaymentId, String eventType, String rawPayload) {
        PaymentWebhookEventEntity event = new PaymentWebhookEventEntity();
        event.id = UUID.randomUUID();
        event.providerEventId = providerEventId;
        event.providerPaymentId = providerPaymentId;
        event.eventType = eventType;
        event.rawPayload = rawPayload;
        event.processingStatus = Constants.RECEIVED;
        event.receivedAt = OffsetDateTime.now();
        return event;
    }

    public void markProcessed() {
        this.processingStatus = Constants.PROCESSED;
        this.processedAt = OffsetDateTime.now();
    }

    public void markIgnoredOutOfOrder(String reason) {
        this.processingStatus = Constants.IGNORED_OUT_OF_ORDER;
        this.errorMessage = reason;
        this.processedAt = OffsetDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.processingStatus = Constants.FAILED;
        this.errorMessage = errorMessage;
    }

}