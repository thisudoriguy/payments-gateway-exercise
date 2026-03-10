package com.checkout.gateway.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {

    AUTHORIZED("Authorized"),
    DECLINED("Declined"),
    REJECTED("Rejected");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
