package com.tgs.school.fees.service;

import com.tgs.school.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Payment gateway abstraction for online fee payments.
 * Verifies signatures server-side using PAYMENT_GATEWAY_KEY_SECRET.
 */
@Service
@Slf4j
public class PaymentGatewayService {

    @Value("${app.payment.provider:MANUAL_ONLY}")
    private String provider;

    @Value("${app.payment.key-secret:}")
    private String keySecret;

    public boolean isOnlineGatewayConfigured() {
        return !"MANUAL_ONLY".equalsIgnoreCase(provider) && keySecret != null && !keySecret.isBlank();
    }

    public String getProviderName() {
        return provider;
    }

    public boolean verifyOnlinePayment(String orderId, String paymentId, String signature) {
        if (!isOnlineGatewayConfigured()) {
            throw new BadRequestException(
                    "Online payment gateway is not configured (PAYMENT_GATEWAY_PROVIDER=" + provider + "). Use Cash, UPI, or Bank Transfer verification.");
        }
        if (orderId == null || paymentId == null || signature == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(raw);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception ex) {
            log.warn("Payment signature verification failed", ex);
            return false;
        }
    }
}
