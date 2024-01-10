package com.agilemall.common.events.update;

import lombok.Data;

@Data
public class CompletedUpdateOrderEvent {
    private String orderId;
    private String orderStatus;
}
