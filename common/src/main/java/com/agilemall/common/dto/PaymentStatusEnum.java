package com.agilemall.common.dto;

import java.util.Arrays;
import java.util.Optional;

public enum PaymentStatusEnum {
    CREATED("10", "결제중"),
    CANCELED("20", "결제취소"),
    COMPLETED("30", "결제완료"),
    ORDER_CANCLLED("50", "주문취소");

    private final String value;
    private final String desc;

    PaymentStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }
    public String description() { return desc; }
    public String description(String value) {
        Optional<PaymentStatusEnum> optStatus = Arrays.stream(PaymentStatusEnum.values())
                .filter(status -> status.value().equals(value))
                .findFirst();
        return optStatus.isPresent()?optStatus.get().description():"";
    }
}
