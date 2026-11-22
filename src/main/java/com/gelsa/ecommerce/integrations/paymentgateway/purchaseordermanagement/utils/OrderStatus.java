package com.gelsa.ecommerce.integrations.paymentgateway.purchaseordermanagement.utils;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    CREATED,
    AWAITING_PAYMENT,
    PAID,
    CANCELLED,
    EXPIRED;

    private static final Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        VALID_TRANSITIONS.put(CREATED, EnumSet.of(AWAITING_PAYMENT, CANCELLED, EXPIRED));
        VALID_TRANSITIONS.put(AWAITING_PAYMENT, EnumSet.of(PAID, CANCELLED, EXPIRED));
        VALID_TRANSITIONS.put(PAID, EnumSet.noneOf(OrderStatus.class));
        VALID_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        VALID_TRANSITIONS.put(EXPIRED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean isValidNextStatus(OrderStatus orderStatus){
        if(this == orderStatus){
            return false;
        }
        return VALID_TRANSITIONS.getOrDefault(this, EnumSet.noneOf(OrderStatus.class)).contains(orderStatus);
    }

    public boolean isEndStatus(){
        return  this == PAID || this == CANCELLED || this == EXPIRED;
    }
}
