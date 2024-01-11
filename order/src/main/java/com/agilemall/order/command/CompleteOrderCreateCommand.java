package com.agilemall.order.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.HashMap;

@Value
@Builder
public class CompleteOrderCreateCommand {
    @TargetAggregateIdentifier
    String orderId;
    String orderStatus;
    HashMap<String, String> aggregateIdMap;
}
