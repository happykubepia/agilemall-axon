package com.agilemall.common.command;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.HashMap;
import java.util.List;

@Data
@Builder
public class CreatePaymentCommand {
    @TargetAggregateIdentifier
    private String paymentId;
    private String orderId;
    private int totalPaymentAmt;
    private List<PaymentDetailDTO> paymentDetails;
    private HashMap<String, String> aggregateIdMap;
}

