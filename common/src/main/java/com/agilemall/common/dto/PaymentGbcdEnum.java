package com.agilemall.common.dto;

public enum PaymentGbcdEnum {
    CARD("10"),
    POINT("20");

    private final String value;

    PaymentGbcdEnum(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
