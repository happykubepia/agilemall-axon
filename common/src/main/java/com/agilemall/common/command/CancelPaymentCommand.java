package com.agilemall.common.command;

import com.agilemall.common.dto.PaymentStatusEnum;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
public class CancelPaymentCommand {
    @TargetAggregateIdentifier
    String paymentId;
    String orderId;
    String paymentStatus = PaymentStatusEnum.CANCELED.value();
}
