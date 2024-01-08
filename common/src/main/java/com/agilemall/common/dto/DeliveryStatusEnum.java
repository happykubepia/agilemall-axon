package com.agilemall.common.dto;

public enum DeliveryStatusEnum {
    CREATED("10"),
    CANCELED("20"),
    DELIVERING("30"),
    COMPLETED("40");

    private final String value;

    DeliveryStatusEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
