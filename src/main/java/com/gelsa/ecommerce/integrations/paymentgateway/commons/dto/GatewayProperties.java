package com.gelsa.ecommerce.integrations.paymentgateway.commons.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "gateway")
public record GatewayProperties (
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String webhookSecret,
        long maxWebhookAgeSeconds
) {
    public GatewayProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(2);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(5);
        }
        if (maxWebhookAgeSeconds <= 0) {
            maxWebhookAgeSeconds = 300;
        }
    }
}
