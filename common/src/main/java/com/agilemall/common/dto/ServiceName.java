package com.agilemall.common.dto;

public enum ServiceName {
    ORDER("order"),
    PAYMENT("payment"),
    DELIVERY("delivery"),
    INVENTORY("inventory"),
    REPORT("report");

    private final String value;
    ServiceName(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
