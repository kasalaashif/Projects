package com.example.paymentservice.service;

import com.example.paymentservice.event.PaymentEvent;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.model.PaymentStatus;
import com.example.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;
    
    private static final String PAYMENT_TOPIC = "payment-events";
    
    @Transactional
    public Payment processPayment(Payment payment) {
        payment.setStatus(PaymentStatus.PROCESSING);
        Payment savedPayment = paymentRepository.save(payment);
        
        // Simulate payment processing
        boolean paymentSuccessful = processPaymentWithProvider(payment);
        
        if (paymentSuccessful) {
            savedPayment.setStatus(PaymentStatus.COMPLETED);
        } else {
            savedPayment.setStatus(PaymentStatus.FAILED);
        }
        
        savedPayment = paymentRepository.save(savedPayment);
        
        // Publish payment event
        PaymentEvent paymentEvent = createPaymentEvent(savedPayment);
        kafkaTemplate.send(PAYMENT_TOPIC, String.valueOf(savedPayment.getOrderId()), paymentEvent);
        
        log.info("Payment processed and event published for orderId: {}", savedPayment.getOrderId());
        return savedPayment;
    }
    
    @Cacheable(value = "payments", key = "#paymentId")
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
    
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order"));
    }
    
    private boolean processPaymentWithProvider(Payment payment) {
        // Simulate payment processing with external provider
        try {
            Thread.sleep(1000); // Simulate processing time
            // In a real implementation, this would integrate with a payment provider
            return Math.random() > 0.1; // 90% success rate
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    private PaymentEvent createPaymentEvent(Payment payment) {
        return PaymentEvent.builder()
                .paymentId(payment.getId())
                .orderId(payment.getOrderId())
                .customerId(payment.getCustomerId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .timestamp(LocalDateTime.now())
                .build();
    }
} 