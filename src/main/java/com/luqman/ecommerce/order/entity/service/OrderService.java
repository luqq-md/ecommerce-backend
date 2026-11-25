package com.luqman.ecommerce.order.entity.service;

import com.luqman.ecommerce.auth.entity.User;
import com.luqman.ecommerce.auth.repository.UserRepository;
import com.luqman.ecommerce.cart.entity.Cart;
import com.luqman.ecommerce.cart.entity.CartItem;
import com.luqman.ecommerce.cart.entity.repository.CartRepository;
import com.luqman.ecommerce.common.exception.BadRequestException;
import com.luqman.ecommerce.common.exception.ConflictException;
import com.luqman.ecommerce.common.exception.ResourceNotFoundException;
import com.luqman.ecommerce.config.RedisConfig;
import com.luqman.ecommerce.order.entity.dto.OrderResponse;
import com.luqman.ecommerce.order.entity.Order;
import com.luqman.ecommerce.order.entity.OrderItem;
import com.luqman.ecommerce.order.entity.repository.OrderRepository;
import com.luqman.ecommerce.product.entity.Product;
import com.luqman.ecommerce.product.entity.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CacheManager cacheManager;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public OrderResponse checkout(Long userId, String rawIdempotencyKey) {
        if (rawIdempotencyKey == null || rawIdempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String idempotencyKey = rawIdempotencyKey.trim();
        if (idempotencyKey.length() > 128) {
            throw new BadRequestException("Idempotency-Key must be at most 128 characters");
        }

        // 1. Idempotency check (retry-safe checkout)
        Optional<Order> existingOrder = orderRepository.findByIdempotencyKeyWithItems(idempotencyKey);
        if (existingOrder.isPresent()) {
            Order existing = existingOrder.get();
            if (!existing.getUser().getId().equals(userId)) {
                throw new ConflictException("Idempotency-Key has already been used");
            }
            return OrderResponse.from(existing);
        }

        // 2. Load cart & verify it is not empty
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new BadRequestException("Cannot checkout with an empty cart"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        // 3. Acquire PESSIMISTIC_WRITE locks in deterministic ID order (prevents overselling & deadlocks)
        List<Long> productIds = cart.getItems().stream()
                .map(ci -> ci.getProduct().getId())
                .sorted()
                .toList();

        Map<Long, Product> lockedProducts = productRepository.findAllByIdInForUpdate(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 4. Validate stock under lock, deduct inventory, and snapshot prices
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = lockedProducts.get(cartItem.getProduct().getId());
            if (product == null || !product.isActive()) {
                throw new ConflictException("Product is no longer available: " + cartItem.getProduct().getName());
            }
            if (product.getStock() < cartItem.getQuantity()) {
                throw new ConflictException(
                        "Insufficient stock for product '%s'. Available: %d, Requested: %d"
                                .formatted(product.getName(), product.getStock(), cartItem.getQuantity()));
            }

            product.setStock(product.getStock() - cartItem.getQuantity());

            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(lineTotal);

            orderItems.add(new OrderItem(
                    product,
                    product.getName(),
                    product.getPrice(),
                    cartItem.getQuantity()
            ));
        }

        // 5. Persist order (with price snapshots) & clear cart atomically
        User user = userRepository.getReferenceById(userId);
        Order order = new Order(UUID.randomUUID().toString(), user, totalAmount, idempotencyKey);
        orderItems.forEach(order::addItem);

        Order savedOrder = orderRepository.save(order);
        cart.clearItems();
        cartRepository.save(cart);

        // 6. Evict affected Redis product caches so stock counts stay fresh
        evictProductCaches(productIds);

        return OrderResponse.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listMyOrders(Long userId) {
        return orderRepository.findAllByUserIdWithItems(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.from(order);
    }

    private void evictProductCaches(List<Long> productIds) {
        Cache productCache = cacheManager.getCache(RedisConfig.PRODUCT_CACHE);
        if (productCache != null) {
            productIds.forEach(productCache::evict);
        }
        Cache pageCache = cacheManager.getCache(RedisConfig.PRODUCT_PAGE_CACHE);
        if (pageCache != null) {
            pageCache.clear();
        }
    }
}