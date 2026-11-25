package com.luqman.ecommerce.cart.entity.service;

import com.luqman.ecommerce.auth.entity.User;
import com.luqman.ecommerce.auth.repository.UserRepository;
import com.luqman.ecommerce.cart.entity.dto.AddToCartRequest;
import com.luqman.ecommerce.cart.entity.dto.CartResponse;
import com.luqman.ecommerce.cart.entity.dto.UpdateCartItemRequest;
import com.luqman.ecommerce.cart.entity.Cart;
import com.luqman.ecommerce.cart.entity.CartItem;
import com.luqman.ecommerce.cart.entity.repository.CartRepository;
import com.luqman.ecommerce.common.exception.ConflictException;
import com.luqman.ecommerce.common.exception.ResourceNotFoundException;
import com.luqman.ecommerce.product.entity.Product;
import com.luqman.ecommerce.product.entity.repository.ProductRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .map(CartResponse::from)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        Product product = productRepository.findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> createCartForUser(userId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst();

        int newQuantity = existingItem
                .map(item -> item.getQuantity() + request.quantity())
                .orElse(request.quantity());

        if (newQuantity > product.getStock()) {
            throw new ConflictException(
                    "Insufficient stock for product '%s'. Available: %d, Requested: %d"
                            .formatted(product.getName(), product.getStock(), newQuantity));
        }

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(newQuantity);
        } else {
            cart.addItem(new CartItem(product, newQuantity));
        }

        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart: " + productId));

        Product product = item.getProduct();
        if (!product.isActive()) {
            throw new ConflictException("Product is no longer available: " + product.getName());
        }
        if (request.quantity() > product.getStock()) {
            throw new ConflictException(
                    "Insufficient stock for product '%s'. Available: %d, Requested: %d"
                            .formatted(product.getName(), product.getStock(), request.quantity()));
        }

        item.setQuantity(request.quantity());
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = cartRepository.findByUserIdWithItems(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart: " + productId));

        cart.removeItem(item);
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public void clearCart(Long userId) {
        cartRepository.findByUserIdWithItems(userId).ifPresent(cart -> {
            cart.clearItems();
            cartRepository.save(cart);
        });
    }

    private Cart createCartForUser(Long userId) {
        User user = userRepository.getReferenceById(userId);
        return cartRepository.save(new Cart(user));
    }
}