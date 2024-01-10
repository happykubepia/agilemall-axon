package com.agilemall.common.dto;

import java.util.Arrays;
import java.util.Optional;

public enum PaymentKindEnum {
    CARD("10", "신용카드"),
    POINT("20", "고객포인트");

    private final String value;
    private final String desc;

    PaymentKindEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }
    public String description() { return desc; }
    public String description(String value) {
        Optional<PaymentKindEnum> optKind = Arrays.stream(PaymentKindEnum.values())
                .filter(kind -> kind.value().equals(value))
                .findFirst();
        return optKind.isPresent()?optKind.get().description():"";
    }
}
