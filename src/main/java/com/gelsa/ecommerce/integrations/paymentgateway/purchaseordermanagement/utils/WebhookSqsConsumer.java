package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.WebhookSqsMessageDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services.PaymentTransitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class WebhookSqsConsumer {

    private final SqsClient sqsClient;
    private final PaymentTransitionService transitionService;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public WebhookSqsConsumer(SqsClient sqsClient, PaymentTransitionService transitionService, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.transitionService = transitionService;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 100)
    public void pollMessages() {
        List<Message> messages = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(20)
                .build()).messages();

        for (Message msg : messages) {
            try {
                WebhookSqsMessageDTO sqsPayload = objectMapper.readValue(msg.body(), WebhookSqsMessageDTO.class);
                transitionService.processWebhookEvent(
                        UUID.fromString(sqsPayload.eventId()),
                        sqsPayload.status(),
                        null
                );

                sqsClient.deleteMessage(DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(msg.receiptHandle())
                        .build());
            } catch (Exception ex) {
                log.error("Error procesando mensaje SQS {}: {}", msg.messageId(), ex.getMessage(), ex);
            }
        }
    }

}
