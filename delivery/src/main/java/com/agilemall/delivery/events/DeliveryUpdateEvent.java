package com.agilemall.delivery.events;

import lombok.Data;

@Data
public class DeliveryUpdateEvent {
    private String deliveryId;
    private String deliveryStatus;
}
