package com.agilemall.delivery.events;

import lombok.Data;

@Data
public class DeliveryUpdatedEvent {
    private String deliveryId;
    private String deliveryStatus;
}
