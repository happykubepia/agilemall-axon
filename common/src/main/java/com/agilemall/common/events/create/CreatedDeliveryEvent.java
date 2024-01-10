package com.agilemall.common.events.create;

import lombok.Data;

import java.util.HashMap;

@Data
public class CreatedDeliveryEvent {
    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
    private HashMap<String, String> aggregateIdMap;
}
