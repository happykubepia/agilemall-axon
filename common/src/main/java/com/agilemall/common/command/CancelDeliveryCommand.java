package com.agilemall.common.command;

import com.agilemall.common.dto.DeliveryStatus;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class CancelDeliveryCommand {
    @TargetAggregateIdentifier
    String deliveryId;
    String orderId;
    String deliveryStatus = DeliveryStatus.CANCELED.value();
}
