package com.agilemall.common.events.create;

import lombok.Data;

@Data
public class CreatedDeliveryEvent {
    private String orderId;
    private String deliveryId;
    private String deliveryStatus;
    private boolean isCompensation;
}
