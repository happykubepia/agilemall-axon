package com.agilemall.delivery.dto;

public enum DeliveryStatus {
    REQUESTED("10"),
    CANCELED("20"),
    COMPLETED("30");

    private String value;

    DeliveryStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
