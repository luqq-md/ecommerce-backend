package com.luqman.ecommerce.messaging.entity.repository;

import com.luqman.ecommerce.messaging.entity.OutboxEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value = """
                    SELECT * FROM outbox_events
                    WHERE status = 'PENDING'
                    ORDER BY created_at ASC
                    LIMIT :limit
                    FOR UPDATE SKIP LOCKED
                    """,
            nativeQuery = true
    )
    List<OutboxEvent> findPendingBatchForUpdate(@Param("limit") int limit);
}