package com.agilemall.common.events.create;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class CreatedPaymentEvent {
    private String paymentId;
    private String orderId;
    private int totalPaymentAmt;
    private List<PaymentDetailDTO> paymentDetails;
    private HashMap<String, String> aggregateIdMap;
}
