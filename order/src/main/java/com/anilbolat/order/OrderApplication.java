package com.anilbolat.order;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

interface OrderRepository extends CrudRepository<Orders, Long> {

}

@SpringBootApplication
@EnableFeignClients
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}

@RestController
@RequiredArgsConstructor
class OrderController {

    private final OrderService orderService;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public void placeOrder(@RequestBody PlaceOrderRequest request) {
        System.out.println("\nrequest: " + request.toString() + "\n");
        this.orderService.placeOrder(request);
    }
}

@Data
@NoArgsConstructor
@AllArgsConstructor
class PlaceOrderRequest {
    private String product;
    private double price;
}

@Service
@RequiredArgsConstructor
class OrderService {

    private final KafkaTemplate kafkaTemplate;
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public void placeOrder(PlaceOrderRequest request) {
        // check if product exists before placing an order
        InventoryStatus status = inventoryClient.exists(request.getProduct());
        if (!status.isExists()) {
            throw new EntityNotFoundException("Product [" + request.getProduct() + "] does not exist.");
        }

        // save into db
        var order = new Orders();
        order.setProduct(request.getProduct());
        order.setPrice(request.getPrice());
        order.setStatus("PLACED");
        Orders o = this.orderRepository.save(order);
        // send to shipping (kafka)
        this.kafkaTemplate.send("prod.orders.placed", String.valueOf(o.getId()), OrderPlacedEvent.builder()
                .product(request.getProduct())
                .price(request.getPrice())
                .orderId(o.getId().intValue())
                .build());
    }

    @KafkaListener(topics = "prod.orders.shipped", groupId = "order-group")
    public void handleOrderShippedEvent(String orderId) {
        this.orderRepository.findById(Long.valueOf(orderId))
                .ifPresent(o -> {
                    o.setStatus("SHIPPED");
                    this.orderRepository.save(o);
                });
    }


}

@Data
@Builder
class OrderPlacedEvent {
    private int orderId;
    private String product;
    private double price;
}

@Entity
@Data
class Orders {

    @Id
    @GeneratedValue
    private Long id;
    private String product;
    private double price;
    private String status;
}

@FeignClient(url = "http://localhost:8092", name = "inventories")
interface InventoryClient {
    @GetMapping("/inventories")
    InventoryStatus exists(@RequestParam("productId") String productId);
}

@Data
class InventoryStatus {
    private boolean exists;
}
