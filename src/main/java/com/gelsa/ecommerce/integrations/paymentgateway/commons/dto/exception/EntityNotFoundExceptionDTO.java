package com.gelsa.ecommerce.integrations.paymentgateway.commons.dto.exception;

public class EntityNotFoundExceptionDTO extends RuntimeException{
    public EntityNotFoundExceptionDTO(String message) {
        super(message);
    }
}
