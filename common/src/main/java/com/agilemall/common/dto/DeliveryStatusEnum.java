package com.agilemall.common.dto;

import java.util.Arrays;
import java.util.Optional;

public enum DeliveryStatusEnum {
    CREATED("10", "준비중"),
    CANCELED("20", "배송취소"),
    DELIVERING("30", "배송중"),
    COMPLETED("40", "배송완료");

    private final String value;
    private final String desc;

    DeliveryStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return this.value;
    }
    public String description() {
        return this.desc;
    }
    public String description(String value) {
        Optional<DeliveryStatusEnum> optRet = Arrays.stream(DeliveryStatusEnum.values())
                .filter(status -> status.value().equals(value))
                .findFirst();

        return (optRet.isPresent() ? optRet.get().description() : "");
    }
}
