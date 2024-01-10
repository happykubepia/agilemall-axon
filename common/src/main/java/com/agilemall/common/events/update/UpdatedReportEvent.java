package com.agilemall.common.events.update;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdatedReportEvent {
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
