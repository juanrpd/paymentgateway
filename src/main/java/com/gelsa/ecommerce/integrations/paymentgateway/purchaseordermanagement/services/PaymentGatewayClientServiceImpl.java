package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.GatewayProperties;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayChargeResultDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.GatewayStatusResultDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class PaymentGatewayClientServiceImpl implements PaymentGatewayClientService {

    private final RestClient restClient;

    public PaymentGatewayClientServiceImpl(GatewayProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public GatewayChargeResultDTO createCharge(UUID orderId, UUID attemptId, String idempotencyKey, String providerName, BigDecimal amount, String currency, String paymentMethodToken) {
        ChargePayload payload = new ChargePayload(orderId, attemptId, providerName, amount, currency, paymentMethodToken);

        try {
            log.info("llamando a la pasarela..");
            return restClient.post()
                    .uri("/gateway/v1/charges")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Idempotency-Key", idempotencyKey)
                    .body(payload)
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();

                        if (status.is2xxSuccessful()) {
                            GatewayChargeApiResponse body = response.bodyTo(GatewayChargeApiResponse.class);
                            if (body == null) {
                                return new GatewayChargeResultDTO(
                                        PaymentAttemptStatus.UNKNOWN_TIMEOUT,
                                        null,
                                        null,
                                        "Respuesta vacía de la pasarela"
                                );
                            }
                            return new GatewayChargeResultDTO(
                                    PaymentAttemptStatus.REDIRECT_REQUIRED,
                                    body.providerPaymentId(),
                                    body.redirectUrl(),
                                    null
                            );
                        }

                        if (status.value() == 400 || status.value() == 422) {
                            GatewayChargeApiResponse errBody = response.bodyTo(GatewayChargeApiResponse.class);
                            String reason = (errBody != null && errBody.message() != null)
                                    ? errBody.message()
                                    : "Pago rechazado por la pasarela (HTTP " + status.value() + ")";
                            return new GatewayChargeResultDTO(
                                    PaymentAttemptStatus.REJECTED,
                                    errBody != null ? errBody.providerPaymentId() : null,
                                    null,
                                    reason
                            );
                        }

                        log.warn("Respuesta inesperada de la pasarela al crear cobro: HTTP {}", status.value());
                        return new GatewayChargeResultDTO(
                                PaymentAttemptStatus.UNKNOWN_TIMEOUT,
                                null,
                                null,
                                "HTTP inesperado de la pasarela: " + status.value()
                        );
                    });
        } catch (RestClientException ex) {
            log.warn("Timeout o error de red al invocar pasarela para attempt {}: {}", attemptId, ex.getMessage());
            return new GatewayChargeResultDTO(
                    PaymentAttemptStatus.UNKNOWN_TIMEOUT,
                    null,
                    null,
                    "Timeout o error de comunicación con la pasarela"
            );
        }
    }

    @Override
    public GatewayStatusResultDTO queryPaymentStatus(String idempotencyKey, String providerPaymentId) {
        try {
            GatewayStatusApiResponse body = restClient.get()
                    .uri(uriBuilder -> {
                        if (providerPaymentId != null && !providerPaymentId.isBlank()) {
                            return uriBuilder.path("/gateway/v1/charges/{paymentId}").build(providerPaymentId);
                        }
                        return uriBuilder.path("/gateway/v1/charges/by-idempotency/{key}").build(idempotencyKey);
                    })
                    .header("Idempotency-Key", idempotencyKey)
                    .retrieve()
                    .body(GatewayStatusApiResponse.class);

            if (body == null || body.status() == null) {
                return new GatewayStatusResultDTO(providerPaymentId, PaymentAttemptStatus.UNKNOWN_TIMEOUT, null);
            }

            return new GatewayStatusResultDTO(
                    body.providerPaymentId() != null ? body.providerPaymentId() : providerPaymentId,
                    body.status(),
                    body.failureReason()
            );
        } catch (RestClientException ex) {
            log.warn("Error consultando estado en pasarela (idempotencyKey={}, providerPaymentId={}): {}",
                    idempotencyKey, providerPaymentId, ex.getMessage());
            return new GatewayStatusResultDTO(providerPaymentId, PaymentAttemptStatus.UNKNOWN_TIMEOUT, null);
        }
    }

    private record ChargePayload(
            UUID orderId,
            UUID attemptId,
            String providerName,
            BigDecimal amount,
            String currency,
            String paymentMethodToken
    ) {}

    private record GatewayChargeApiResponse(
            String providerPaymentId,
            String redirectUrl,
            String message
    ) {}

    private record GatewayStatusApiResponse(
            String providerPaymentId,
            PaymentAttemptStatus status,
            String failureReason
    ) {}
}
