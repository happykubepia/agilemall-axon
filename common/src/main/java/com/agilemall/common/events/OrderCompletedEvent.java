package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;

@Data
@Builder
public class OrderCompletedEvent {
    private String orderId;
    private String orderStatus;
    private HashMap<String, String> aggregateIdMap;
}
