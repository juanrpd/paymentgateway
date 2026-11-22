package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEventEntity, UUID> {

    Optional<PaymentWebhookEventEntity> findByProviderEventId(String providerEventId);
}
