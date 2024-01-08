package com.agilemall.common.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Data;

import java.util.HashMap;
import java.util.List;

@Data
public class PaymentCreatedEvent {
    private String paymentId;
    private String orderId;
    private int totalPaymentAmt;
    private List<PaymentDetailDTO> paymentDetails;
    private HashMap<String, String> aggregateIdMap;
}
