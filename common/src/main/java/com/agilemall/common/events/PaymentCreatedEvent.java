package com.agilemall.common.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentCreatedEvent {
    private String paymentId;
    private String orderId;
    private int totalPaymentAmt;
    private List<PaymentDetailDTO> paymentDetails;
    private HashMap<String, String> aggregateIdMap;
}
