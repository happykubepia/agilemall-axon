package com.agilemall.common.events.update;

import lombok.Data;

@Data
public class CancelledUpdateOrderEvent {
    private String orderId;
}
