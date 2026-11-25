package com.tgs.school.notifications.service;

public interface SmsProvider {
    String sendSms(String phoneNumber, String message);
}
