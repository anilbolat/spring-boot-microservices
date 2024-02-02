package com.anilbolat.inventory.controller;

import com.anilbolat.inventory.model.InventoryStatus;
import com.anilbolat.inventory.util.TraceUtils;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Observed
public class InventoryController {

    private final Tracer tracer;

    // in-memory db
    private final Map<String, InventoryStatus> statues = Map.of("1", new InventoryStatus(true), "2", new InventoryStatus(false));

    @GetMapping(value = "/inventories", produces = MediaType.APPLICATION_JSON_VALUE)
    public InventoryStatus getInventoryStatus(@RequestParam("productId") String productId) {
        log.info("getting inventory status for product id: {}", productId);
        TraceUtils.addTag(this.tracer.currentSpan(), "request.product.id", productId);

        return this.statues.getOrDefault(productId, new InventoryStatus(false));
    }
}
