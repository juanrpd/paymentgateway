package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.controller;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.CreateOrderRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.InitiatePaymentRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.OrderResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.PaymentAttemptResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services.OrderService;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services.PaymentInitiationService;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.PaymentAttemptStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentInitiationService paymentInitiationService;

    public OrderController(OrderService orderService, PaymentInitiationService paymentInitiationService) {
        this.orderService = orderService;
        this.paymentInitiationService = paymentInitiationService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@Valid @RequestBody CreateOrderRequestDTO request) {
        OrderResponseDTO response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @PostMapping("/{orderId}/payments")
    public ResponseEntity<PaymentAttemptResponseDTO> initiatePayment(
            @PathVariable UUID orderId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiatePaymentRequestDTO request
    ) {
        PaymentAttemptResponseDTO response = paymentInitiationService.initiatePayment(orderId, idempotencyKey, request);
        if (response.status() == PaymentAttemptStatus.REJECTED) {
            return ResponseEntity.unprocessableEntity().body(response);
        }
        return ResponseEntity.accepted().body(response);
    }
}
