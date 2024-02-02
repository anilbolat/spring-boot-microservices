package com.anilbolat.order.client;

import com.anilbolat.order.model.InventoryStatus;
import io.micrometer.observation.annotation.Observed;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(url = "http://localhost:8092", name = "inventories")
@Observed
public interface InventoryClient {
    @GetMapping("/inventories")
    InventoryStatus exists(@RequestParam("productId") String productId);
}
