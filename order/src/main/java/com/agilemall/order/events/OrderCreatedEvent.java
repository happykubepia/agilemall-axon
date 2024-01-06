package com.agilemall.order.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.OrderDetailDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private LocalDateTime orderDatetime;
    private String orderStatus;
    private int totalOrderAmt;
    private List<OrderDetailDTO> orderDetails;
    private String paymentId;
    private List<PaymentDetailDTO> paymentDetails;
    private int totalPaymentAmt;

}
