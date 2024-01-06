package com.agilemall.common.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.HashMap;

@Value
@Builder
public class DeliveryOrderCommand {
    @TargetAggregateIdentifier
    String deliveryId;
    String orderId;
    private HashMap<String, String> aggregateIdMap;
}
