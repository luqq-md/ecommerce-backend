package com.luqman.ecommerce.order.entity.controller;

import com.luqman.ecommerce.auth.security.UserPrincipal;
import com.luqman.ecommerce.order.entity.dto.OrderResponse;
import com.luqman.ecommerce.order.entity.service.OrderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        OrderResponse order = orderService.checkout(principal.getId(), idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> listMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.listMyOrders(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getMyOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getMyOrder(principal.getId(), id));
    }
}