package com.agilemall.common.events;

import lombok.Data;

@Data
public class ReportDeliveryStatusUpdatedEvent {
    private String orderId;
    private String deliveryStatus;
}
