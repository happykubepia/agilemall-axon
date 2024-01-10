package com.agilemall.common.command.update;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class UpdateReportDeliveryStatusCommand {
    @TargetAggregateIdentifier
    String reportId;
    String orderId;
    String deliveryStatus;
}
