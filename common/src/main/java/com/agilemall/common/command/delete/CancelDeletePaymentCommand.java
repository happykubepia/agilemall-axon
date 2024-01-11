package com.agilemall.common.command.delete;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CancelDeletePaymentCommand {
    @TargetAggregateIdentifier
    String paymentId;
    String orderId;
    boolean isCompensation;
}
