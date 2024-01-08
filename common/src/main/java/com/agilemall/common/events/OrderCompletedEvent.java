package com.agilemall.common.events;

import lombok.Data;

import java.util.HashMap;

@Data
public class OrderCompletedEvent {
    private String orderId;
    private String orderStatus;
    private HashMap<String, String> aggregateIdMap;
}
