package com.agilemall.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CompletedDeleteOrderEvent {
    private String orderId;
}
