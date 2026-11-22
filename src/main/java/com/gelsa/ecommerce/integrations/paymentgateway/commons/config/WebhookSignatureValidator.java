package com.gelsa.ecommerce.integrations.paymentgateway.commons.config;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.GatewayProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Component
public class WebhookSignatureValidator {
    private static final String HMAC_ALGO = "HmacSHA256";

    private final GatewayProperties properties;

    public WebhookSignatureValidator(GatewayProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String timestampHeader, String signatureHeader, String rawBody) {
        if (timestampHeader == null || signatureHeader == null || rawBody == null) {
            return false;
        }

        long eventTimestamp;
        try {
            eventTimestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            return false;
        }

        long nowSeconds = Instant.now().getEpochSecond();
        if (Math.abs(nowSeconds - eventTimestamp) > properties.maxWebhookAgeSeconds()) {
            return false;
        }

        String signedPayload = timestampHeader.trim() + "." + rawBody;
        String expectedHex = computeHmacHex(signedPayload, properties.webhookSecret());
        String providedNormalized = signatureHeader.trim().toLowerCase();

        return MessageDigest.isEqual(expectedHex.getBytes(StandardCharsets.UTF_8), providedNormalized.getBytes(StandardCharsets.UTF_8));
    }

    private String computeHmacHex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(rawHmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Error calculando HMAC-SHA256", ex);
        }
    }
}
