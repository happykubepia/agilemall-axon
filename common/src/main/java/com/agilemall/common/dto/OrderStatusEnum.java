package com.agilemall.common.dto;

public enum OrderStatusEnum {
    CREATED("10"),
    CANCELED("20"),
    COMPLETED("30"),
    APPROVED("40");

    private final String value;

    OrderStatusEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
