package com.anilbolat.shipping;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.KafkaListeners;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@SpringBootApplication
public class ShippingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShippingApplication.class, args);
    }

}

@Service
@RequiredArgsConstructor
class OrderService {

    private final KafkaTemplate kafkaTemplate;
    private final ShippingRepository shippingRepository;

    @KafkaListener(topics = "prod.orders.placed", groupId = "shipping-group")
    public void handleOrderService(OrderPlaceEvent event) {
        // save into db
        var s = new Shipping();
        s.setOrderId(event.getOrderId());
        this.shippingRepository.save(s);

        // send kafka
        this.kafkaTemplate.send("prod.orders.shipped", String.valueOf(s.getOrderId()), String.valueOf(s.getOrderId()));
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class OrderPlaceEvent {
    private int orderId;
    private String product;
    private double price;
}

@Entity
@Data
class Shipping {

    @Id
    @GeneratedValue
    private Long id;
    private int orderId;
}

interface ShippingRepository extends CrudRepository<Shipping, Long> {

}
