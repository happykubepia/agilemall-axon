package com.agilemall.common.dto;

public enum PaymentGbcd {
    CARD("10"),
    POINT("20");

    private final String value;

    PaymentGbcd(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
