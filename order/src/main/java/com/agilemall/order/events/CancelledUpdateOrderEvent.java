package com.agilemall.order.events;

import lombok.Data;

@Data
public class CancelledUpdateOrderEvent {
    private String orderId;
}
