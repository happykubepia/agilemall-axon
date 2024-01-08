package com.agilemall.report.entity;

import com.agilemall.common.command.CreateReportCommand;
import com.agilemall.common.command.UpdateReportDeliveryStatusCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.ReportCreatedEvent;
import com.agilemall.common.events.ReportDeliveryStatusUpdatedEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Slf4j
@Aggregate
@Data
@Entity
@Table(name = "report")
public class Report implements Serializable {
    @Serial
    private static final long serialVersionUID = -2315607591930989462L;

    @TargetAggregateIdentifier
    @Id
    @Column(name = "report_id", nullable = false, length = 15)
    private String reportId;

    @Column(name = "order_id", nullable = false, length = 15)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 30)
    private String userId;

    @Column(name = "order_datetime", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDatetime;

    @Column(name = "total_order_amt", nullable = false)
    private int totalOrderAmt;

    @Column(name = "order_status", nullable = false, length = 2)
    private String orderStatus;

    @Column(name = "order_details", nullable = true, length = 3000)
    private String orderDetails;

    @Column(name = "payment_id", nullable = false, length = 15)
    private String paymentId;

    @Column(name = "total_payment_amt", nullable = false)
    private int totalPaymentAmt;

    @Column(name = "payment_status", nullable = false, length = 2)
    private String paymentStatus;

    @Column(name = "payment_details", nullable = true, length = 3000)
    private String paymentDetails;

    @Column(name = "delivery_id", nullable = false, length = 15)
    private String deliveryId;

    @Column(name = "delivery_status", nullable = false, length = 2)
    private String deliveryStatus;

    public Report() { }

    @CommandHandler
    private Report(CreateReportCommand createReportCommand) {
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
    private void on(ReportCreatedEvent event) {
        log.info("[@EventSourcingHandler] Executing <ReportCreatedEvent> for Report Id: {}", event.getReportId());

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        this.reportId = event.getReportId();
        this.orderId = event.getOrderId();
        this.userId = event.getUserId();
        this.orderDatetime = event.getOrderDatetime();
        this.totalOrderAmt = event.getTotalOrderAmt();
        this.orderStatus = event.getOrderStatus();
        this.orderDetails = gson.toJson(event.getOrderDetails());
        this.paymentId = event.getPaymentId();
        this.totalPaymentAmt = event.getTotalPaymentAmt();
        this.paymentStatus = event.getPaymentStatus();
        this.paymentDetails = gson.toJson(event.getPaymentDetails());
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
}
