package com.agilemall.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class PaymentDTO {
    private String orderId;
    private String paymentId;
    private int totalPaymentAmt;
    private PaymentStatus paymentStatus;
    private List<PaymentDetailDTO> paymentDetails;
}
