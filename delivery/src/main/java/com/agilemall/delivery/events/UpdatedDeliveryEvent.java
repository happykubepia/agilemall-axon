package com.agilemall.delivery.events;

import lombok.Data;

@Data
public class UpdatedDeliveryEvent {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
