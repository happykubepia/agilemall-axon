package com.agilemall.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDetailDTO {
    private String orderId;
    private String paymentId;
    private String paymentGbcd;
    private int paymentAmt;
}
