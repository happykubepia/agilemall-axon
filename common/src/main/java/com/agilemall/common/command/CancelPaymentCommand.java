package com.agilemall.common.command;

import com.agilemall.common.dto.PaymentStatus;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class CancelPaymentCommand {
    @TargetAggregateIdentifier
    String paymentId;
    String orderId;
    String paymentStatus = PaymentStatus.CANCELED.value();
}
