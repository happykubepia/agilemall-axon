package com.agilemall.common.command.update;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CancelUpdatePaymentCommand {
    @TargetAggregateIdentifier
    String paymentId;
    String orderId;
    boolean isCompensation;
}
