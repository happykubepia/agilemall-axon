package com.agilemall.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderReqCreateDTO {
    private String userId;
    private List<OrderReqDetailDTO> orderReqDetails;
    private List<PaymentReqDetailDTO> paymentReqDetails;
}
