package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum PaymentAttemptStatus {
    INITIATED,
    REDIRECT_REQUIRED,
    PENDING_PROVIDER,
    UNKNOWN_TIMEOUT,
    APPROVED,
    REJECTED;

    private static final Map<PaymentAttemptStatus, Set<PaymentAttemptStatus>> VALID_TRANSITIONS = new EnumMap<>(PaymentAttemptStatus.class);

    static {
        VALID_TRANSITIONS.put(INITIATED, EnumSet.of(REDIRECT_REQUIRED, PENDING_PROVIDER, UNKNOWN_TIMEOUT, APPROVED, REJECTED));
        VALID_TRANSITIONS.put(REDIRECT_REQUIRED, EnumSet.of(PENDING_PROVIDER, UNKNOWN_TIMEOUT, APPROVED, REJECTED));
        VALID_TRANSITIONS.put(PENDING_PROVIDER, EnumSet.of(UNKNOWN_TIMEOUT, APPROVED, REJECTED));
        VALID_TRANSITIONS.put(UNKNOWN_TIMEOUT, EnumSet.of(PENDING_PROVIDER, APPROVED, REJECTED));

        VALID_TRANSITIONS.put(APPROVED, EnumSet.noneOf(PaymentAttemptStatus.class));
        VALID_TRANSITIONS.put(REJECTED, EnumSet.noneOf(PaymentAttemptStatus.class));
    }

    public boolean isValidNextStatus(PaymentAttemptStatus paymentAttemptStatus){
        if(this == paymentAttemptStatus){
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(PaymentAttemptStatus.class)).contains(paymentAttemptStatus);
    }

    public boolean isEndStatus(){
        return  this == APPROVED || this == REJECTED;
    }
}
