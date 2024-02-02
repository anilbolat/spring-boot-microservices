package com.anilbolat.shipping.service;

import com.anilbolat.shipping.model.OrderPlacedEvent;
import com.anilbolat.shipping.model.Shipping;
import com.anilbolat.shipping.repository.ShippingRepository;
import com.anilbolat.shipping.util.TraceUtils;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String KAFKA_TOPIC_ORDERS_PLACED = "prod.orders.placed";
    private static final String KAFKA_TOPIC_ORDERS_SHIPPED = "prod.orders.shipped";


    private final KafkaTemplate kafkaTemplate;
    private final ShippingRepository shippingRepository;
    private final Tracer tracer;

    @Observed
    @KafkaListener(topics = KAFKA_TOPIC_ORDERS_PLACED, groupId = "shipping-group")
    public void handleOrderService(OrderPlacedEvent event) {
        log.info("an order is placed: {}", event);
        TraceUtils.addTag(this.tracer.currentSpan(), "kafka.received.from." + KAFKA_TOPIC_ORDERS_PLACED, event.toString());

        log.info("saving into db.");
        var s = new Shipping();
        s.setOrderId(event.getOrderId());
        this.shippingRepository.save(s);

        log.info("sending kafka.");
        var key = String.valueOf(s.getOrderId());
        var value = String.valueOf(s.getOrderId());
        var producerRecord = new ProducerRecord<>(KAFKA_TOPIC_ORDERS_SHIPPED, key, value);
        TraceUtils.addTag(this.tracer.currentSpan(), "kafka.sending", producerRecord.toString());

        this.kafkaTemplate.send(KAFKA_TOPIC_ORDERS_SHIPPED, String.valueOf(s.getOrderId()), String.valueOf(s.getOrderId()));
    }
}