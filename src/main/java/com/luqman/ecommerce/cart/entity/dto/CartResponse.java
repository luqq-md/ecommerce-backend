// CartResponse.java
package com.luqman.ecommerce.cart.entity.dto;

import com.luqman.ecommerce.cart.entity.Cart;
import com.luqman.ecommerce.cart.entity.dto.CartItemResponse;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        int totalItems,
        BigDecimal totalAmount
) {
    public static CartResponse empty() {
        return new CartResponse(List.of(), 0, BigDecimal.ZERO);
    }

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        int totalItems = itemResponses.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal totalAmount = itemResponses.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(itemResponses, totalItems, totalAmount);
    }
}