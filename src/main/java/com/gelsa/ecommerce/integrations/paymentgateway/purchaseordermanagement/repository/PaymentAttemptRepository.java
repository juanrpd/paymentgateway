package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {

    Optional<PaymentAttemptEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentAttemptEntity> findByProviderPaymentId(String providerPaymentId);

    List<PaymentAttemptEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PaymentAttemptEntity p WHERE p.id = :id")
    Optional<PaymentAttemptEntity> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT p FROM PaymentAttemptEntity p
            WHERE p.status IN :statuses
              AND p.retryCount < :maxRetries
              AND p.updatedAt <= :olderThan
            ORDER BY p.updatedAt ASC
            """)
    List<PaymentAttemptEntity> findCandidatesForReconciliation(
            @Param("statuses") Collection<PaymentAttemptStatus> statuses,
            @Param("maxRetries") int maxRetries,
            @Param("olderThan") OffsetDateTime olderThan
    );
}
