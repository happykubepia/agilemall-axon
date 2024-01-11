package com.agilemall.order.events;

import lombok.Data;

@Data
public class CompletedDeleteOrderEvent {
    private String orderId;
}
