package com.agilemall.common.dto;

import java.util.Arrays;
import java.util.Optional;

public enum OrderStatusEnum {
    CREATED("10", "주문접수"),
    FAILED("20", "주문에러"),
    COMPLETED("30", "주문등록"),
    UPTATED("40", "주문수정"),
    ORDER_CANCLLED("50", "주문취소");

    private final String value;
    private final String desc;

    OrderStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public String value() {
        return value;
    }

    public String description() { return desc; }
    public String description(String value) {
        Optional<OrderStatusEnum> optStatus = Arrays.stream(OrderStatusEnum.values())
                .filter(status -> status.value().equals(value))
                .findFirst();
        return optStatus.isPresent() ? optStatus.get().description():"";
    }
}
