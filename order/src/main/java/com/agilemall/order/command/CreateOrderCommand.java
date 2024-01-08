package com.agilemall.order.command;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class CreateOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    String userId;
    LocalDateTime orderDatetime;
    String orderStatus;
    int totalOrderAmt;
    List<OrderDetailDTO> orderDetails;
    String paymentId;
    List<PaymentDetailDTO> paymentDetails;
    int totalPaymentAmt;

}
