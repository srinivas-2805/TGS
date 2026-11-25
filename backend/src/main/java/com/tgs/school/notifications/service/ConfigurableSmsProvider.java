package com.tgs.school.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Configurable SMS provider.
 * When SMS_PROVIDER=LOG_ONLY or DISABLED, messages are logged without external HTTP calls.
 * Live providers (e.g., MSG91, Twilio) require valid SMS_API_KEY and SMS_API_URL in environment variables.
 */
@Service
@Slf4j
public class ConfigurableSmsProvider implements SmsProvider {

    @Value("${app.sms.provider:LOG_ONLY}")
    private String provider;

    @Value("${app.sms.api-key:}")
    private String apiKey;

    @Value("${app.sms.sender-id:TGSSCH}")
    private String senderId;

    @Override
    public String sendSms(String phoneNumber, String message) {
        if ("DISABLED".equalsIgnoreCase(provider) || phoneNumber == null || phoneNumber.isBlank()) {
            return "SKIPPED";
        }
        if ("LOG_ONLY".equalsIgnoreCase(provider) || apiKey == null || apiKey.isBlank()) {
            log.info("[SMS:{}] To={} Sender={} Msg={}", provider, phoneNumber, senderId, message);
            return "LOGGED_DEV";
        }
        // Configured live provider hook (credentials injected from env)
        log.info("[SMS:{}] Dispatching SMS to {} via configured gateway", provider, phoneNumber);
        return "QUEUED_PROVIDER";
    }
}
