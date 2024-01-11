package com.agilemall.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancelledDeleteOrderEvent {
    private String orderId;
}
