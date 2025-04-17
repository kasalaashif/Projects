package com.example.inventoryservice.service;

import com.example.inventoryservice.event.InventoryEvent;
import com.example.inventoryservice.model.InventoryItem;
import com.example.inventoryservice.model.InventoryReservation;
import com.example.inventoryservice.model.ReservationStatus;
import com.example.inventoryservice.repository.InventoryItemRepository;
import com.example.inventoryservice.repository.InventoryReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryReservationRepository reservationRepository;
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;
    
    private static final String INVENTORY_TOPIC = "inventory-events";
    
    @Transactional
    public InventoryReservation reserveInventory(Long orderId, Map<String, Integer> productQuantities) {
        // Check if all products are available
        List<InventoryItem> items = inventoryItemRepository.findByProductIds(
                productQuantities.keySet().stream().toList());
        
        boolean allAvailable = items.stream().allMatch(item -> 
                item.getAvailableQuantity() >= productQuantities.get(item.getProductId()));
        
        InventoryReservation reservation = InventoryReservation.builder()
                .orderId(orderId)
                .status(allAvailable ? ReservationStatus.CONFIRMED : ReservationStatus.CANCELLED)
                .build();
        
        if (allAvailable) {
            // Reserve inventory
            items.forEach(item -> {
                item.setReservedQuantity(item.getReservedQuantity() + 
                        productQuantities.get(item.getProductId()));
                inventoryItemRepository.save(item);
            });
        }
        
        reservation = reservationRepository.save(reservation);
        
        // Publish inventory event
        InventoryEvent event = createInventoryEvent(reservation, items, productQuantities);
        kafkaTemplate.send(INVENTORY_TOPIC, String.valueOf(orderId), event);
        
        return reservation;
    }
    
    @Transactional
    public void confirmReservation(Long orderId) {
        InventoryReservation reservation = getReservation(orderId);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);
    }
    
    @Transactional
    public void cancelReservation(Long orderId) {
        InventoryReservation reservation = getReservation(orderId);
        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            // Release reserved inventory
            String productId = reservation.getProductId();
            InventoryItem item = inventoryItemRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Inventory item not found"));
            
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getQuantity());
            inventoryItemRepository.save(item);
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
    
    @Cacheable(value = "inventory", key = "#productId")
    public InventoryItem getInventoryItem(String productId) {
        return inventoryItemRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory item not found"));
    }
    
    @CacheEvict(value = "inventory", key = "#productId")
    public InventoryItem updateInventory(String productId, Integer quantity) {
        InventoryItem item = getInventoryItem(productId);
        item.setQuantity(quantity);
        return inventoryItemRepository.save(item);
    }
    
    private InventoryReservation getReservation(Long orderId) {
        return reservationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }
    
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredReservations() {
        List<InventoryReservation> expiredReservations = 
                reservationRepository.findByExpiresAtBeforeAndStatus(
                        LocalDateTime.now(), ReservationStatus.PENDING.name());
        
        expiredReservations.forEach(reservation -> {
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            
            // Release reserved inventory
            String productId = reservation.getProductId();
            InventoryItem item = inventoryItemRepository.findByProductId(productId)
                    .orElseThrow(() -> new RuntimeException("Inventory item not found"));
            
            item.setReservedQuantity(item.getReservedQuantity() - reservation.getQuantity());
            inventoryItemRepository.save(item);
        });
    }
    
    private InventoryEvent createInventoryEvent(
            InventoryReservation reservation,
            List<InventoryItem> items,
            Map<String, Integer> productQuantities) {
        return InventoryEvent.builder()
                .orderId(reservation.getOrderId())
                .status(reservation.getStatus())
                .timestamp(LocalDateTime.now())
                .items(items.stream()
                        .map(item -> InventoryEvent.InventoryItemEvent.builder()
                                .productId(item.getProductId())
                                .quantity(productQuantities.get(item.getProductId()))
                                .available(item.getAvailableQuantity() >= 
                                        productQuantities.get(item.getProductId()))
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
} 