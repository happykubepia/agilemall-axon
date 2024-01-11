package com.agilemall.common.command.create;

import com.agilemall.common.dto.OrderStatusEnum;
import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CancelCreateOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    String orderStatus = OrderStatusEnum.FAILED.value();
}
