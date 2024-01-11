package com.agilemall.common.events.delete;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancelledDeleteDeliveryEvent {
    private String deliveryId;
    private String orderId;
    private boolean isCompensation;
}
