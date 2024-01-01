package com.agilemall.common.command;

import com.agilemall.common.dto.OrderStatus;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class CancelOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    String orderStatus = OrderStatus.CANCELED.value();
}
