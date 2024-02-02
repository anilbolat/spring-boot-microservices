package com.anilbolat.order.service;

import com.anilbolat.order.client.InventoryClient;
import com.anilbolat.order.model.InventoryStatus;
import com.anilbolat.order.model.OrderPlacedEvent;
import com.anilbolat.order.model.Orders;
import com.anilbolat.order.model.PlaceOrderRequest;
import com.anilbolat.order.model.enums.ORDER_STATUS;
import com.anilbolat.order.repository.OrderRepository;
import com.anilbolat.order.util.TraceUtils;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {

    private static final String KAFKA_TOPIC_ORDERS_PLACED = "prod.orders.placed";
    private static final String KAFKA_TOPIC_ORDERS_SHIPPED = "prod.orders.shipped";

    private final KafkaTemplate kafkaTemplate;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final Tracer tracer;

    @Observed
    public void placeOrder(PlaceOrderRequest request) {
        log.info("checking if product {} exists before placing an order.", request.getProduct());
        TraceUtils.addTag(this.tracer.currentSpan(), "place.order.request", request.toString());

        InventoryStatus status = inventoryClient.exists(request.getProduct());
        if (!status.isExists()) {
            String msg = "Product [" + request.getProduct() + "] does not exist.";
            log.info(msg);
            throw new EntityNotFoundException(msg);
        }

        log.info("saving into db.");
        var order = new Orders();
        order.setProduct(request.getProduct());
        order.setPrice(request.getPrice());
        order.setStatus(ORDER_STATUS.PLACED.toString());
        Orders o = this.orderRepository.save(order);

        String key = String.valueOf(o.getId());
        OrderPlacedEvent value = OrderPlacedEvent.builder()
                .product(request.getProduct())
                .price(request.getPrice())
                .orderId(o.getId().intValue())
                .build();
        log.info("sending to shipping (kafka).");

        String kafkaMsg = "kafka - topic: " + KAFKA_TOPIC_ORDERS_PLACED + ", key: " + key + ", value: " + value;
        log.debug(kafkaMsg);
        TraceUtils.addTag(this.tracer.currentSpan(), "kafka.sending.msg", kafkaMsg);

        this.kafkaTemplate.send(KAFKA_TOPIC_ORDERS_PLACED, key, value);
    }

    @Observed
    @KafkaListener(topics = KAFKA_TOPIC_ORDERS_SHIPPED, groupId = "order-group")
    public void handleOrderShippedEvent(String orderId) {
        log.info("an order is shipped: order id is {}", orderId);
        TraceUtils.addTag(this.tracer.currentSpan(), "kafka.received.from." + KAFKA_TOPIC_ORDERS_SHIPPED, orderId);

        this.orderRepository.findById(Long.valueOf(orderId))
                .ifPresent(o -> {
                    o.setStatus(ORDER_STATUS.SHIPPED.toString());
                    this.orderRepository.save(o);
                });
    }
}
