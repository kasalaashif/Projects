package com.example.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockSmsService {
    private static final Logger log = LoggerFactory.getLogger(MockSmsService.class);

    public void sendSms(String to, String content) {
        log.info("Simulating SMS sending to: {}", to);
        log.info("Content: {}", content);
    }
}