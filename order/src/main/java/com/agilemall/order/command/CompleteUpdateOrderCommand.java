package com.agilemall.order.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CompleteUpdateOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    String orderStatus;
}