package com.agilemall.order.events;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FailedUpdateOrderEvent {
    private String orderId;
}
