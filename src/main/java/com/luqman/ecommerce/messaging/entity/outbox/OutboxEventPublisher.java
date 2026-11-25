package com.luqman.ecommerce.messaging.entity.outbox;

import com.luqman.ecommerce.messaging.entity.OutboxEvent;
import com.luqman.ecommerce.messaging.entity.event.OrderCreatedEvent;
import com.luqman.ecommerce.messaging.entity.repository.OutboxEventRepository;
import com.luqman.ecommerce.order.entity.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OutboxEventPublisher {

    public static final String AGGREGATE_ORDER = "ORDER";
    public static final String EVENT_ORDER_CREATED = "ORDER_CREATED";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository, JsonMapper jsonMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishOrderCreated(Order order) {
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        String payload = jsonMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                AGGREGATE_ORDER,
                order.getOrderNumber(),
                EVENT_ORDER_CREATED,
                payload
        );
        outboxEventRepository.save(outboxEvent);
    }
}