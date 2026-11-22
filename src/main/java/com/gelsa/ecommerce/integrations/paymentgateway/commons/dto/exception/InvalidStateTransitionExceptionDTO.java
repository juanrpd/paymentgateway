package com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.exception;

public class InvalidStateTransitionExceptionDTO extends RuntimeException {
    public InvalidStateTransitionExceptionDTO(String message) {
        super(message);
    }
}
