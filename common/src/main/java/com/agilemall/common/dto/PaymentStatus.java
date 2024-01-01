package com.agilemall.common.dto;

public enum PaymentStatus {
    CREATED("10"),
    CANCELED("20"),
    COMPLETED("30");

    private final String value;

    PaymentStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
