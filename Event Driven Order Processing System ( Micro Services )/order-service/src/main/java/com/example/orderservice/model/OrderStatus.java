package com.example.orderservice.model;

public enum OrderStatus {
    CREATED,
    PENDING_INVENTORY_CHECK,
    INVENTORY_CONFIRMED,
    PAYMENT_PENDING,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    CANCELLED,
    COMPLETED
} 