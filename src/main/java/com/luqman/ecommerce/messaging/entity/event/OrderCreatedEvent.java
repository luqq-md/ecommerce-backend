package com.luqman.ecommerce.messaging.entity.event;

import com.luqman.ecommerce.order.entity.Order;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        String orderNumber,
        Long userId,
        BigDecimal totalAmount,
        List<Item> items,
        Instant occurredAt
) {
    public record Item(Long productId, String productName, BigDecimal unitPrice, Integer quantity) {}

    public static OrderCreatedEvent from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(
                        i.getProduct().getId(),
                        i.getProductName(),
                        i.getUnitPrice(),
                        i.getQuantity()))
                .toList();

        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getOrderNumber(),
                order.getUser().getId(),
                order.getTotalAmount(),
                items,
                Instant.now()
        );
    }
}