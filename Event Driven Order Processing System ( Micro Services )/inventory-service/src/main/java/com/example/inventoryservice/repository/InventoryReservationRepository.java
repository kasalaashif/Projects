package com.example.inventoryservice.repository;

import com.example.inventoryservice.model.InventoryReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {
    Optional<InventoryReservation> findByOrderId(Long orderId);
    List<InventoryReservation> findByExpiresAtBeforeAndStatus(LocalDateTime dateTime, String status);
} 