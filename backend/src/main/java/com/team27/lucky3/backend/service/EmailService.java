package com.team27.lucky3.backend.service;

public interface EmailService {
    void sendSimpleMessage(String to, String subject, String text);
}