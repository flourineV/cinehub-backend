package com.cinehub.fnb.controller;

import com.cinehub.fnb.dto.request.FnbOrderRequest;
import com.cinehub.fnb.dto.response.FnbOrderResponse;
import com.cinehub.fnb.service.FnbOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/fnb/orders")
@RequiredArgsConstructor
public class FnbOrderController {

    private final FnbOrderService fnbOrderService;

    @PostMapping
    public ResponseEntity<FnbOrderResponse> createOrder(@RequestBody FnbOrderRequest request) {
        return new ResponseEntity<>(fnbOrderService.createOrder(request), HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FnbOrderResponse>> getOrdersByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(fnbOrderService.getOrdersByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FnbOrderResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(fnbOrderService.getById(id));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable UUID id) {
        fnbOrderService.cancelOrder(id);
        return ResponseEntity.noContent().build();
    }
}
