package com.agilemall.common.dto;

public enum ServiceNameEnum {
    ORDER("order"),
    PAYMENT("payment"),
    DELIVERY("delivery"),
    INVENTORY("inventory"),
    REPORT("report");

    private final String value;
    ServiceNameEnum(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
