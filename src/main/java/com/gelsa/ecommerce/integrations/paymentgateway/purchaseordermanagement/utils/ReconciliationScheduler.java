package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayStatusResultDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAttemptRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services.PaymentGatewayClientService;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services.PaymentTransitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class ReconciliationScheduler {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentGatewayClientService paymentGatewayClient;
    private final PaymentTransitionService paymentTransitionService;

    private static final Set<PaymentAttemptStatus> TRANSIENT_STATUSES = EnumSet.of(
            PaymentAttemptStatus.INITIATED,
            PaymentAttemptStatus.REDIRECT_REQUIRED,
            PaymentAttemptStatus.PENDING_PROVIDER,
            PaymentAttemptStatus.UNKNOWN_TIMEOUT
    );

    private static final int MAX_RETRIES = 10;

    public ReconciliationScheduler(PaymentAttemptRepository paymentAttemptRepository, PaymentGatewayClientService paymentGatewayClient, PaymentTransitionService paymentTransitionService) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentGatewayClient = paymentGatewayClient;
        this.paymentTransitionService = paymentTransitionService;
    }

    @Scheduled(fixedDelay = 10_000)
    public void reconcileTransientAttempts() {
        OffsetDateTime olderThan = OffsetDateTime.now().minusSeconds(60);
        List<PaymentAttemptEntity> candidates = paymentAttemptRepository.findCandidatesForReconciliation(
                TRANSIENT_STATUSES,
                MAX_RETRIES,
                olderThan
        );

        for (PaymentAttemptEntity attempt : candidates) {
            GatewayStatusResultDTO statusResult = paymentGatewayClient.queryPaymentStatus(
                    attempt.getIdempotencyKey(),
                    attempt.getProviderPaymentId()
            );

            if (statusResult.status() == PaymentAttemptStatus.UNKNOWN_TIMEOUT) {
                paymentAttemptRepository.findById(attempt.getId()).ifPresent(a -> {
                    a.incrementRetryCount();
                    paymentAttemptRepository.save(a);
                });
                continue;
            }

            paymentTransitionService.processReconciliationResult(
                    attempt.getId(),
                    statusResult.status(),
                    statusResult.failureReason()
            );
        }
    }
}
