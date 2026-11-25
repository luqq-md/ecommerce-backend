package com.luqman.ecommerce.cart.entity.repository;

import com.luqman.ecommerce.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @Query("""
            SELECT DISTINCT c FROM Cart c
            LEFT JOIN FETCH c.items ci
            LEFT JOIN FETCH ci.product
            WHERE c.user.id = :userId
            """)
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}