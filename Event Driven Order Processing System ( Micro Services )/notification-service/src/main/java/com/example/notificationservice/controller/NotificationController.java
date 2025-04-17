package com.example.notificationservice.controller;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestParam Long orderId,
            @RequestParam String customerId,
            @RequestParam NotificationType type,
            @RequestParam String recipient,
            @RequestParam String subject,
            @RequestParam String content) {
        Notification notification = notificationService.createNotification(
                orderId, customerId, type, recipient, subject, content);
        return new ResponseEntity<>(notification, HttpStatus.CREATED);
    }
    
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<Notification>> getNotificationsByOrderId(@PathVariable Long orderId) {
        List<Notification> notifications = notificationService.getNotificationsByOrderId(orderId);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        notificationService.sendEmailNotification(
            request.getRecipient(),
            request.getSubject(),
            request.getContent()
        );
        return ResponseEntity.ok("Email notification sent successfully");
    }

    @PostMapping("/sms")
    public ResponseEntity<String> sendSms(@RequestBody SmsRequest request) {
        notificationService.sendSmsNotification(
            request.getRecipient(),
            request.getContent()
        );
        return ResponseEntity.ok("SMS notification sent successfully");
    }

    @Data
    @Getter
    @Setter
    public static class EmailRequest {
        private String recipient;
        private String subject;
        private String content;
    }

    @Data
    @Getter
    @Setter
    public static class SmsRequest {
        private String recipient;
        private String content;
    }
} 