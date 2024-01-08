package com.agilemall.common.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateReportDeliveryStatusCommand {
    String orderId;
    String deliveryStatus;
}
