package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "payment_attempt_id")
    private UUID paymentAttemptId;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(name = "actor_source", nullable = false, length = 64)
    private String actorSource;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static PaymentAuditLogEntity create(UUID orderId, UUID paymentAttemptId, String previousStatus, String newStatus, String actorSource, String traceId, String metadataJson) {
        PaymentAuditLogEntity entry = new PaymentAuditLogEntity();
        entry.orderId = orderId;
        entry.paymentAttemptId = paymentAttemptId;
        entry.previousStatus = previousStatus;
        entry.newStatus = newStatus;
        entry.actorSource = actorSource;
        entry.traceId = traceId;
        entry.metadata = metadataJson;
        entry.createdAt = OffsetDateTime.now();
        return entry;
    }
}
