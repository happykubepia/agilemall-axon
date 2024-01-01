package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderDeliveriedEvent {
    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
}
