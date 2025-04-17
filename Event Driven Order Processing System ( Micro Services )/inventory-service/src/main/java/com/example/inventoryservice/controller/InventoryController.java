package com.example.inventoryservice.controller;

import com.example.inventoryservice.model.InventoryItem;
import com.example.inventoryservice.model.InventoryReservation;
import com.example.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    
    @PostMapping("/reserve")
    public ResponseEntity<InventoryReservation> reserveInventory(
            @RequestParam Long orderId,
            @RequestBody Map<String, Integer> productQuantities) {
        InventoryReservation reservation = inventoryService.reserveInventory(orderId, productQuantities);
        return new ResponseEntity<>(reservation, HttpStatus.CREATED);
    }
    
    @PostMapping("/reserve/{orderId}/confirm")
    public ResponseEntity<Void> confirmReservation(@PathVariable Long orderId) {
        inventoryService.confirmReservation(orderId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/reserve/{orderId}/cancel")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long orderId) {
        inventoryService.cancelReservation(orderId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{productId}")
    public ResponseEntity<InventoryItem> getInventoryItem(@PathVariable String productId) {
        InventoryItem item = inventoryService.getInventoryItem(productId);
        return ResponseEntity.ok(item);
    }
    
    @PutMapping("/{productId}")
    public ResponseEntity<InventoryItem> updateInventory(
            @PathVariable String productId,
            @RequestParam Integer quantity) {
        InventoryItem item = inventoryService.updateInventory(productId, quantity);
        return ResponseEntity.ok(item);
    }
} 