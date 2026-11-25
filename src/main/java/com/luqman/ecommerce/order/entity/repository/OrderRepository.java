package com.luqman.ecommerce.order.entity.repository;

import com.luqman.ecommerce.order.entity.Order;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.idempotencyKey = :key")
    Optional<Order> findByIdempotencyKeyWithItems(@Param("key") String idempotencyKey);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.user.id = :userId
            ORDER BY o.createdAt DESC
            """)
    List<Order> findAllByUserIdWithItems(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items
            WHERE o.id = :orderId AND o.user.id = :userId
            """)
    Optional<Order> findByIdAndUserIdWithItems(@Param("orderId") Long orderId, @Param("userId") Long userId);
}