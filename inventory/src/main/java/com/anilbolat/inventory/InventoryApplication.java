package com.anilbolat.inventory;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }

}

@RestController
class InventoryController {

    // in-memory db
    private final Map<String, InventoryStatus> statues = Map.of("1", new InventoryStatus(true), "2", new InventoryStatus(false));

    @GetMapping(
            value = "/inventories",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public InventoryStatus getInventoryStatus(@RequestParam("productId") String productId) {
        System.out.println("\nproductId: " + productId + "\n");
        return this.statues.getOrDefault(productId, new InventoryStatus(false));
    }
}

@Data
@AllArgsConstructor
class InventoryStatus {
    private boolean exists;
}
