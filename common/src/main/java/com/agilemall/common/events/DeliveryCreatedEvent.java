package com.agilemall.common.events;

import lombok.Data;

import java.util.HashMap;

@Data
public class DeliveryCreatedEvent {
    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
    private HashMap<String, String> aggregateIdMap;
}
