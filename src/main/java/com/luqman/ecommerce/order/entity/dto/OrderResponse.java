// OrderResponse.java
package com.luqman.ecommerce.order.entity.dto;

import com.luqman.ecommerce.order.entity.Order;
import com.luqman.ecommerce.order.entity.OrderStatus;
import com.luqman.ecommerce.order.entity.dto.OrderItemResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalAmount,
        String idempotencyKey,
        List<OrderItemResponse> items,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getIdempotencyKey(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getCreatedAt()
        );
    }
}