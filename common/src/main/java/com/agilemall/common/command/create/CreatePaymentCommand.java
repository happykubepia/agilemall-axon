package com.agilemall.common.command.create;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Value
@Builder
public class CreatePaymentCommand {
    @TargetAggregateIdentifier
    String paymentId;
    String orderId;
    int totalPaymentAmt;
    List<PaymentDetailDTO> paymentDetails;
    boolean isCompensation;
}

