package com.agilemall.common.events.update;

import lombok.Data;

@Data
public class UpdatedReportDeliveryStatusEvent {
    private String reportId;
    private String orderId;
    private String deliveryStatus;
}
