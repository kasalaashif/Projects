package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.model.NotificationStatus;
import com.example.notificationservice.model.NotificationType;
import com.example.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    
    private final NotificationRepository notificationRepository;
    private final MockEmailService emailService;
    private final MockSmsService smsService;

    public NotificationService(NotificationRepository notificationRepository,
                             MockEmailService emailService,
                             MockSmsService smsService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    @Transactional
    public Notification createNotification(Long orderId, String customerId, NotificationType type,
                                         String recipient, String subject, String content) {
        Notification notification = new Notification();
        notification.setOrderId(orderId);
        notification.setCustomerId(customerId);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setType(type);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRetryCount(0);

        try {
            switch (type) {
                case EMAIL:
                    emailService.sendEmail(recipient, subject, content);
                    break;
                case SMS:
                    smsService.sendSms(recipient, content);
                    break;
            }
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send notification to {}: {}", recipient, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }

        return notificationRepository.save(notification);
    }

    public List<Notification> getNotificationsByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId);
    }

    @Transactional
    public void sendEmailNotification(String recipient, String subject, String content) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setType(NotificationType.EMAIL);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRetryCount(0);

        try {
            emailService.sendEmail(recipient, subject, content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", recipient, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }

    @Transactional
    public void sendSmsNotification(String recipient, String content) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setContent(content);
        notification.setType(NotificationType.SMS);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRetryCount(0);

        try {
            smsService.sendSms(recipient, content);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
        } catch (Exception e) {
            log.error("Failed to send SMS notification to {}: {}", recipient, e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }
} 