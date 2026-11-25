package com.luqman.ecommerce.messaging.entity.consumer;

import com.luqman.ecommerce.config.KafkaConfig;
import com.luqman.ecommerce.messaging.entity.event.OrderCreatedEvent;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final JsonMapper jsonMapper;
    // In multi-instance production, store processed eventIds in a DB table or Redis SET with TTL
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public OrderEventConsumer(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_TOPIC, groupId = "notification-service")
    public void handleOrderCreated(
            String payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        OrderCreatedEvent event = jsonMapper.readValue(payload, OrderCreatedEvent.class);

        if (!processedEventIds.add(event.eventId())) {
            log.info("Skipping duplicate eventId={} for orderNumber={}",
                    event.eventId(), event.orderNumber());
            return;
        }

        log.info("Consumed ORDER_CREATED [partition={}, offset={}]: orderNumber={}, userId={}, totalAmount={}, items={}",
                partition, offset, event.orderNumber(), event.userId(), event.totalAmount(), event.items().size());
    }

    @KafkaListener(topics = KafkaConfig.ORDER_EVENTS_DLT, groupId = "notification-service-dlt")
    public void handleDeadLetter(
            String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.error("Received message in DLT [key={}]: {}", key, payload);
    }
}