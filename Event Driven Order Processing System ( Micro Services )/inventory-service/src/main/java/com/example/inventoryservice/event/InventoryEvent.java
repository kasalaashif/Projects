package com.example.inventoryservice.event;

import com.example.inventoryservice.model.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryEvent {
    private Long orderId;
    private ReservationStatus status;
    private LocalDateTime timestamp;
    private List<InventoryItemEvent> items;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemEvent {
        private String productId;
        private Integer quantity;
        private Boolean available;
    }
} 