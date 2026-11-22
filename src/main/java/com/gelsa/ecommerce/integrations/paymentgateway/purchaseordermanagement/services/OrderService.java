package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.CreateOrderRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.OrderResponseDTO;

import java.util.UUID;

public interface OrderService {
    OrderResponseDTO createOrder(CreateOrderRequestDTO request);

    OrderResponseDTO getOrder(UUID orderId);
}
