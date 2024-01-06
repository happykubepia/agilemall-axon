package com.agilemall.delivery.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class DeliveryUpdateCommand {
    @TargetAggregateIdentifier
    String deliveryId;

    String deliveryStatus;
}