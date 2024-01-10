package com.agilemall.common.events.create;

import lombok.Data;

@Data
public class CancelledCreateDeliveryEvent {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
