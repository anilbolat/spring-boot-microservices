package com.anilbolat.order.controller;

import com.anilbolat.order.model.PlaceOrderRequest;
import com.anilbolat.order.service.OrderService;
import com.anilbolat.order.util.TraceUtils;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@Observed
public class OrderController {

    private final OrderService orderService;
    private final Tracer tracer;

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public void placeOrder(@RequestBody PlaceOrderRequest request) {
        log.info("place order request: " + request.toString());
        TraceUtils.addTag(this.tracer.currentSpan(), "place.order.request", request.toString());

        this.orderService.placeOrder(request);
    }
}
