package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.controller;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.config.WebhookSignatureValidator;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.WebhookPayloadRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.WebhookSqsMessageDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentWebhookEventEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentWebhookEventRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.WebhookSqsProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/webhooks/payments")
public class WebhookController {

    private final WebhookSignatureValidator signatureValidator;
    private final PaymentWebhookEventRepository webhookEventRepository;
    private final WebhookSqsProducer sqsProducer;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookSignatureValidator signatureValidator, PaymentWebhookEventRepository webhookEventRepository, WebhookSqsProducer sqsProducer, ObjectMapper objectMapper) {
        this.signatureValidator = signatureValidator;
        this.webhookEventRepository = webhookEventRepository;
        this.sqsProducer = sqsProducer;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/{provider}")
    public ResponseEntity<Void> receiveWebhook(
            @PathVariable String provider,
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestHeader(value = "X-Timestamp", required = false) String timestamp,
            @RequestBody String rawBody
    ) {
        if (!signatureValidator.isValid(timestamp, signature, rawBody)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        WebhookPayloadRequestDTO payload = objectMapper.readValue(rawBody, WebhookPayloadRequestDTO.class);

        Optional<PaymentWebhookEventEntity> existing = webhookEventRepository.findByProviderEventId(payload.eventId());
        if (existing.isPresent()) {
            return ResponseEntity.ok().build();
        }

        PaymentWebhookEventEntity savedEvent = webhookEventRepository.save(
                PaymentWebhookEventEntity.create(
                        payload.eventId(),
                        payload.paymentId(),
                        payload.eventType(),
                        rawBody
                )
        );

        sqsProducer.enqueueWebhookEvent(new WebhookSqsMessageDTO(
                savedEvent.getId().toString(),
                payload.paymentId(),
                payload.status()
        ));

        return ResponseEntity.ok().build();
    }
}
