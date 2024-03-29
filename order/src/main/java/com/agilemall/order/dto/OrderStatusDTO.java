package com.agilemall.order.dto;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class OrderStatusDTO {
    String orderId;
    String userId;
    LocalDateTime orderDatetime;
    int totalOrderAmt;
    String orderStatus;
    List<OrderDetailDTO> orderDetails;
    String paymentId;
    int totalPaymentAmt;
    String paymentStatus;
    List<PaymentDetailDTO> paymentDetails;
    String deliveryId;
    String deliveryStatus;
}
