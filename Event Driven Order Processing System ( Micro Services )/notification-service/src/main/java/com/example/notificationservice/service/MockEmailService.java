package com.example.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockEmailService {
    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    public void sendEmail(String to, String subject, String content) {
        log.info("Simulating email sending to: {}", to);
        log.info("Subject: {}", subject);
        log.info("Content: {}", content);
    }
} 