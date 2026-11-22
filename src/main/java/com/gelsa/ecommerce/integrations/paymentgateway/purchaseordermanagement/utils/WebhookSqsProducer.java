package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.WebhookSqsMessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.ObjectMapper;

@Component
public class WebhookSqsProducer {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    public WebhookSqsProducer(SqsClient sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    public void enqueueWebhookEvent(WebhookSqsMessageDTO message) {
        String json = objectMapper.writeValueAsString(message);
        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(json)
                .build());
    }
}
