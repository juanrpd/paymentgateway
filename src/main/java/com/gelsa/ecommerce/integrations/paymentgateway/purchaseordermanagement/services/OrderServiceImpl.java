package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.services;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.CreateOrderRequestDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.OrderResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.mappers.PaymentMapper;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.OrderEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAuditLogEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.OrderRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAttemptRepository;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.repository.PaymentAuditLogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentAuditLogRepository paymentAuditLogRepository;
    private final PaymentMapper paymentMapper;

    public OrderServiceImpl(OrderRepository orderRepository, PaymentAttemptRepository paymentAttemptRepository, PaymentAuditLogRepository paymentAuditLogRepository, PaymentMapper paymentMapper) {
        this.orderRepository = orderRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentAuditLogRepository = paymentAuditLogRepository;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    @Override
    public OrderResponseDTO createOrder(CreateOrderRequestDTO request) {
        OrderEntity order = OrderEntity.create(request.customerId(), request.amount(), request.currency().toUpperCase());
        OrderEntity saved = orderRepository.save(order);

        paymentAuditLogRepository.save(PaymentAuditLogEntity.create(
                saved.getId(),
                null,
                null,
                saved.getStatus().name(),
                "ORDER_CREATION",
                MDC.get("traceId"),
                null
        ));

        return paymentMapper.toOrderResponse(saved, List.of());
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDTO getOrder(UUID orderId) {
        OrderEntity order = orderRepository.findById(orderId).orElseThrow(() -> new EntityNotFoundException("Orden no encontrada: " + orderId));
        List<PaymentAttemptEntity> attempts = paymentAttemptRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return paymentMapper.toOrderResponse(order, attempts);
    }
}
