package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

@Data
@Builder
public class OrderDeliveredEvent {
    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
    private HashMap<String, String> aggregateIdMap;
}
