package com.agilemall.order.events;

import lombok.Data;

@Data
public class CancelledCreateOrderEvent {
    private String orderId;
    private String orderStatus;
}
