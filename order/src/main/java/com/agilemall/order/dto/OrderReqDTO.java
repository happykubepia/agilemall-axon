package com.agilemall.order.dto;

import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderReqDTO {
    private String userId;
    private List<OrderReqDetailDTO> orderReqDetails;
    private List<PaymentReqDetailDTO> paymentReqDetails;
}
