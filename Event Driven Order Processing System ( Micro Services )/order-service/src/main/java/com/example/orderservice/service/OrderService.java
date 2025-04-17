package com.example.orderservice.service;

import com.example.orderservice.event.OrderEvent;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    
    private static final String ORDER_TOPIC = "order-events";
    
    @Transactional
    public Order createOrder(Order order) {
        order.setStatus(OrderStatus.CREATED);
        Order savedOrder = orderRepository.save(order);
        
        // Publish order created event
        OrderEvent orderEvent = createOrderEvent(savedOrder);
        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(savedOrder.getId()), orderEvent);
        
        log.info("Order created and event published for orderId: {}", savedOrder.getId());
        return savedOrder;
    }
    
    @Cacheable(value = "orders", key = "#orderId")
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = getOrder(orderId);
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);
        
        // Publish order status updated event
        OrderEvent orderEvent = createOrderEvent(updatedOrder);
        kafkaTemplate.send(ORDER_TOPIC, String.valueOf(orderId), orderEvent);
        
        log.info("Order status updated to {} for orderId: {}", status, orderId);
        return updatedOrder;
    }
    
    private OrderEvent createOrderEvent(Order order) {
        return OrderEvent.builder()
                .orderId(order.getId())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .timestamp(LocalDateTime.now())
                .items(order.getItems().stream()
                        .map(item -> OrderEvent.OrderItemEvent.builder()
                                .productId(item.getProductId())
                                .quantity(item.getQuantity())
                                .price(item.getPrice())
                                .subtotal(item.getSubtotal())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
} 