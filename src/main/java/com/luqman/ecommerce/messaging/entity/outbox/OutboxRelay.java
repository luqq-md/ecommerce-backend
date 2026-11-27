package com.luqman.ecommerce.messaging.entity.outbox; // keep .entity.outbox if you haven't moved it yet

import com.luqman.ecommerce.config.KafkaConfig;
import com.luqman.ecommerce.messaging.entity.OutboxEvent;
import com.luqman.ecommerce.messaging.entity.repository.OutboxEventRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final int maxRetries;
    private final Map<UUID, Integer> retryCounts = new ConcurrentHashMap<>();

    public OutboxRelay(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${app.outbox.batch-size:50}") int batchSize,
            @Value("${app.outbox.max-retries:5}") int maxRetries) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:2000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> batch = outboxEventRepository.findPendingBatchForUpdate(batchSize);
        if (batch.isEmpty()) {
            return;
        }

        for (OutboxEvent event : batch) {
            try {
                kafkaTemplate
                        .send(KafkaConfig.ORDER_EVENTS_TOPIC, event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.markPublished();
                retryCounts.remove(event.getId());
                log.info("Relayed outbox event id={} type={} aggregateId={}",
                        event.getId(), event.getEventType(), event.getAggregateId());
            } catch (Exception ex) {
                int attempts = retryCounts.merge(event.getId(), 1, Integer::sum);
                if (attempts >= maxRetries) {
                    event.markFailed();
                    retryCounts.remove(event.getId());
                    log.error("Outbox event id={} permanently failed after {} attempts",
                            event.getId(), attempts, ex);
                } else {
                    log.warn("Failed to relay outbox event id={} (attempt {}/{}): {}",
                            event.getId(), attempts, maxRetries, ex.getMessage());
                }
            }
        }
    }
}