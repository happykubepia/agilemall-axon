package com.agilemall.order.events;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdatedOrderEvent {
    private String orderId;
    private String paymentId;
    private LocalDateTime orderDatetime;
    private int totalOrderAmt;
    private List<OrderDetailDTO> orderDetails;
    private List<PaymentDetailDTO> paymentDetails;
    private int totalPaymentAmt;
    private String orderStatus;
    private boolean isCompensation;
}
