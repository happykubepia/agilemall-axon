package com.agilemall.order.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedCompleteDeleteOrderEvent {
    private String orderId;
}
