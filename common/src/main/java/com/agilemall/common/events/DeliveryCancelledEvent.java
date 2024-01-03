package com.agilemall.common.events;

import lombok.Data;

@Data
public class DeliveryCancelledEvent {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
