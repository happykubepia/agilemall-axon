package com.agilemall.common.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.HashMap;

@Value
@Builder
public class CreateDeliveryCommand {
    @TargetAggregateIdentifier
    String deliveryId;
    String orderId;
    String deliveryStatus;
    HashMap<String, String> aggregateIdMap;
}
