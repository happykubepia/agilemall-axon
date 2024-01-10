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
public class UpdateOrderCommand {
    @TargetAggregateIdentifier
    String orderId;
    LocalDateTime orderDatetime;
    int totalOrderAmt;
    String paymentId;
    List<OrderDetailDTO> orderDetails;
    List<PaymentDetailDTO> paymentDetails;
    int totalPaymentAmt;
    boolean isCompensation;
}
