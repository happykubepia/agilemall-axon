package com.agilemall.order.events;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeletedOrderEvent {
    private String orderId;
    private boolean isCompensation;
}
