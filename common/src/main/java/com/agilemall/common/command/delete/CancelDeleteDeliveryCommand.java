package com.agilemall.common.command.delete;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CancelDeleteDeliveryCommand {
    @TargetAggregateIdentifier
    String deleveryId;
    String orderId;
    boolean isCompensation;
}
