package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.mappers;

import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.OrderResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.dto.PaymentAttemptResponseDTO;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.OrderEntity;
import com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.models.PaymentAttemptEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentMapper {
    public PaymentAttemptResponseDTO toAttemptResponse(PaymentAttemptEntity paymentAttemptEntity) {
        String message = switch (paymentAttemptEntity.getStatus()) {
            case REDIRECT_REQUIRED -> "Redirige al cliente para completar el pago";
            case PENDING_PROVIDER -> "Pago pendiente de confirmación por el proveedor";
            case UNKNOWN_TIMEOUT -> "El pago está en verificación por demora de respuesta del proveedor";
            case APPROVED -> "Pago aprobado";
            case REJECTED -> paymentAttemptEntity.getFailureReason() != null && !paymentAttemptEntity.getFailureReason().isBlank()? paymentAttemptEntity.getFailureReason() : "Pago rechazado por el proveedor";
            case INITIATED -> "Intento de pago iniciado";
        };

        return new PaymentAttemptResponseDTO(
                paymentAttemptEntity.getId(),
                paymentAttemptEntity.getOrder().getId(),
                paymentAttemptEntity.getIdempotencyKey(),
                paymentAttemptEntity.getProviderPaymentId(),
                paymentAttemptEntity.getProviderName(),
                paymentAttemptEntity.getAmount(),
                paymentAttemptEntity.getStatus(),
                paymentAttemptEntity.getRedirectUrl(),
                message,
                paymentAttemptEntity.getUpdatedAt()
        );
    }

    public OrderResponseDTO toOrderResponse(OrderEntity order, List<PaymentAttemptEntity> attemptEntityList) {
        List<PaymentAttemptResponseDTO> attemptDtoList = attemptEntityList.stream().map(this::toAttemptResponse).toList();

        return new OrderResponseDTO(
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getStatus(),
                attemptDtoList,
                order.getCreatedAt()
        );
    }
}
