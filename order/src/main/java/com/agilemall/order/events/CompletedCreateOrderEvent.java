package com.agilemall.order.events;

import lombok.Data;

@Data
public class CompletedCreateOrderEvent {
    private String orderId;
    private String orderStatus;
}
