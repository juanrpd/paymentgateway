package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models;

import com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.exception.InvalidStateTransitionExceptionDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static OrderEntity create(String customerId, BigDecimal totalAmount, String currency) {
        OrderEntity order = new OrderEntity();
        order.id = UUID.randomUUID();
        order.customerId = customerId;
        order.totalAmount = totalAmount;
        order.currency = currency;
        order.status = OrderStatus.CREATED;
        order.createdAt = OffsetDateTime.now();
        order.updatedAt = order.createdAt;
        return order;
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.isValidNextStatus(newStatus)) {
            throw new InvalidStateTransitionExceptionDTO(String.format("Transición inválida en Order %s: %s -> %s", this.id, this.status, newStatus));
        }
        this.status = newStatus;
        this.updatedAt = OffsetDateTime.now();
    }
}