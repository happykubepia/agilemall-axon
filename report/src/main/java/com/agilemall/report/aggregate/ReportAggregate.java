package com.agilemall.report.aggregate;

import com.agilemall.common.command.CreateReportCommand;
import com.agilemall.common.command.UpdateReportDeliveryStatusCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.ReportCreatedEvent;
import com.agilemall.common.events.ReportDeliveryStatusUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Slf4j
public class ReportAggregate {
    @AggregateIdentifier
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

    public ReportAggregate() { }

    @CommandHandler
    public ReportAggregate(CreateReportCommand createReportCommand) {
        log.info("[@CommandHandler] Executing CreateReportCommand for Report Id: {}", createReportCommand.getReportId());
        log.info(createReportCommand.toString());

        ReportCreatedEvent reportCreatedEvent = new ReportCreatedEvent();
        BeanUtils.copyProperties(createReportCommand, reportCreatedEvent);
        List<OrderDetailDTO> newOrderDetails = createReportCommand.getOrderDetails().stream()
                .map(o -> new OrderDetailDTO(o.getOrderId(), o.getProductId(), o.getOrderSeq(), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        reportCreatedEvent.setOrderDetails(newOrderDetails);
        List<PaymentDetailDTO> newDetails = createReportCommand.getPaymentDetails().stream()
                .map(o -> new PaymentDetailDTO(o.getOrderId(), o.getPaymentId(), o.getPaymentGbcd(), o.getPaymentAmt()))
                .collect(Collectors.toList());
        reportCreatedEvent.setPaymentDetails(newDetails);

        apply(reportCreatedEvent);
    }

    @EventSourcingHandler
    public void on(ReportCreatedEvent event) {
        log.info("[@EventSourcingHandler] Executing <ReportCreatedEvent> for Report Id: {}", event.getReportId());

        this.reportId = event.getReportId();
        this.orderId = event.getOrderId();
        this.userId = event.getUserId();
        this.orderDatetime = event.getOrderDatetime();
        this.totalOrderAmt = event.getTotalOrderAmt();
        this.orderStatus = event.getOrderStatus();
        this.orderDetails = event.getOrderDetails();
        this.paymentId = event.getPaymentId();
        this.totalPaymentAmt = event.getTotalPaymentAmt();
        this.paymentStatus = event.getPaymentStatus();
        this.paymentDetails = event.getPaymentDetails();
        this.deliveryId = event.getDeliveryId();
        this.deliveryStatus = event.getDeliveryStatus();

    }

    @CommandHandler
    private void handle(UpdateReportDeliveryStatusCommand cmd) {
        log.info("[@CommandHandler] Handle <UpdateReportDeliveryStatusCommand> for Order Id: {}", cmd.getOrderId());
        ReportDeliveryStatusUpdatedEvent event = new ReportDeliveryStatusUpdatedEvent();
        BeanUtils.copyProperties(cmd, event);
        apply(event);
    }

    @EventSourcingHandler
    private void on(ReportDeliveryStatusUpdatedEvent event) {
        log.info("[@EventSourcingHandler] Handle <ReportDeliveryStatusUpdatedEvent> for Order Id: {}", event.getOrderId());

        this.deliveryStatus = event.getDeliveryStatus();
    }
}
