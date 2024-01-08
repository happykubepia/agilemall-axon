package com.agilemall.common.events;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ReportCreatedEvent {
    private String reportId;
    private String orderId;
    private String userId;
    private LocalDateTime orderDatetime;
    private int totalOrderAmt;
    private String orderStatus;
    private List<OrderDetailDTO> orderDetails;
    private String paymentId;
    private int totalPaymentAmt;
    private String paymentStatus;
    private List<PaymentDetailDTO> paymentDetails;
    private String deliveryId;
    private String deliveryStatus;
}
