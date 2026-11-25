// CartItemResponse.java
package com.luqman.ecommerce.cart.entity.dto;

import com.luqman.ecommerce.cart.entity.CartItem;
import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String sku,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item) {
        BigDecimal unitPrice = item.getProduct().getPrice();
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                unitPrice,
                item.getQuantity(),
                lineTotal
        );
    }
}