package com.agilemall.common.dto;

public enum DeliveryStatus {
    CREATED("10"),
    CANCELED("20"),
    COMPLETED("30");

    private final String value;

    DeliveryStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
