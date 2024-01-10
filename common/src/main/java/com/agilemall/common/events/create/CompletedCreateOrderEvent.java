package com.agilemall.common.events.create;

import lombok.Data;

import java.util.HashMap;

@Data
public class CompletedCreateOrderEvent {
    private String orderId;
    private String orderStatus;
    private HashMap<String, String> aggregateIdMap;
}
