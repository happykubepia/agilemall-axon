package com.agilemall.report.entity;

import com.agilemall.common.command.create.CreateReportCommand;
import com.agilemall.common.command.update.UpdateReportCommand;
import com.agilemall.common.command.update.UpdateReportDeliveryStatusCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.create.CreatedReportEvent;
import com.agilemall.common.events.update.UpdatedReportDeliveryStatusEvent;
import com.agilemall.common.events.update.UpdatedReportEvent;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateMember;
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

    @AggregateIdentifier
    @Id
    @Column(name = "report_id", nullable = false, length = 15)
    private String reportId;

    @AggregateMember
    @Column(name = "order_id", nullable = false, length = 15)
    private String orderId;

    @AggregateMember
    @Column(name = "user_id", nullable = false, length = 30)
    private String userId;

    @AggregateMember
    @Column(name = "order_datetime", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDatetime;

    @AggregateMember
    @Column(name = "total_order_amt", nullable = false)
    private int totalOrderAmt;

    @AggregateMember
    @Column(name = "order_status", nullable = false, length = 2)
    private String orderStatus;

    @AggregateMember
    @Column(name = "order_details", nullable = true, length = 3000)
    private String orderDetails;

    @AggregateMember
    @Column(name = "payment_id", nullable = false, length = 15)
    private String paymentId;

    @AggregateMember
    @Column(name = "total_payment_amt", nullable = false)
    private int totalPaymentAmt;

    @AggregateMember
    @Column(name = "payment_status", nullable = false, length = 2)
    private String paymentStatus;

    @AggregateMember
    @Column(name = "payment_details", nullable = true, length = 3000)
    private String paymentDetails;

    @AggregateMember
    @Column(name = "delivery_id", nullable = false, length = 15)
    private String deliveryId;

    @AggregateMember
    @Column(name = "delivery_status", nullable = false, length = 2)
    private String deliveryStatus;

    public Report() { }

    @CommandHandler
    private Report(CreateReportCommand createReportCommand) {
        log.info("[@CommandHandler] Executing CreateReportCommand for Report Id: {}", createReportCommand.getReportId());
        log.info(createReportCommand.toString());

        //--State Stored Aggregator 는 자신의 상태 업데이트를 CommandHandler 에서 수행
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        this.reportId = createReportCommand.getReportId();
        this.orderId = createReportCommand.getOrderId();
        this.userId = createReportCommand.getUserId();
        this.orderDatetime = createReportCommand.getOrderDatetime();
        this.totalOrderAmt = createReportCommand.getTotalOrderAmt();
        this.orderStatus = createReportCommand.getOrderStatus();
        this.orderDetails = gson.toJson(createReportCommand.getOrderDetails());
        this.paymentId = createReportCommand.getPaymentId();
        this.totalPaymentAmt = createReportCommand.getTotalPaymentAmt();
        this.paymentStatus = createReportCommand.getPaymentStatus();
        this.paymentDetails = gson.toJson(createReportCommand.getPaymentDetails());
        this.deliveryId = createReportCommand.getDeliveryId();
        this.deliveryStatus = createReportCommand.getDeliveryStatus();

        //-- Event 발행
        CreatedReportEvent createdReportEvent = new CreatedReportEvent();
        BeanUtils.copyProperties(createReportCommand, createdReportEvent);
        List<OrderDetailDTO> newOrderDetails = createReportCommand.getOrderDetails().stream()
                .map(o -> new OrderDetailDTO(o.getOrderId(), o.getProductId(), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        createdReportEvent.setOrderDetails(newOrderDetails);
        List<PaymentDetailDTO> newDetails = createReportCommand.getPaymentDetails().stream()
                .map(o -> new PaymentDetailDTO(o.getOrderId(), o.getPaymentId(), o.getPaymentKind(), o.getPaymentAmt()))
                .collect(Collectors.toList());
        createdReportEvent.setPaymentDetails(newDetails);

        AggregateLifecycle.apply(createdReportEvent);
    }

    @CommandHandler
    private void handle(UpdateReportCommand updateReportCommand) {
        log.info("[@CommandHandler] Executing UpdateReportCommand for Report Id: {}", updateReportCommand.getReportId());
        log.info(updateReportCommand.toString());

        //--State Stored Aggregator 는 자신의 상태 업데이트를 CommandHandler 에서 수행
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        this.orderDatetime = updateReportCommand.getOrderDatetime();
        this.totalOrderAmt = updateReportCommand.getTotalOrderAmt();
        this.orderStatus = updateReportCommand.getOrderStatus();
        this.orderDetails = gson.toJson(updateReportCommand.getOrderDetails());
        this.totalPaymentAmt = updateReportCommand.getTotalPaymentAmt();
        this.paymentStatus = updateReportCommand.getPaymentStatus();
        this.paymentDetails = gson.toJson(updateReportCommand.getPaymentDetails());
        this.deliveryId = updateReportCommand.getDeliveryId();
        this.deliveryStatus = updateReportCommand.getDeliveryStatus();

        //-- Event 발행
        UpdatedReportEvent updatedReportEvent = new UpdatedReportEvent();
        BeanUtils.copyProperties(updateReportCommand, updatedReportEvent);

        List<OrderDetailDTO> newOrderDetails = updateReportCommand.getOrderDetails().stream()
                .map(o -> new OrderDetailDTO(o.getOrderId(), o.getProductId(), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        updatedReportEvent.setOrderDetails(newOrderDetails);
        List<PaymentDetailDTO> newDetails = updateReportCommand.getPaymentDetails().stream()
                .map(o -> new PaymentDetailDTO(o.getOrderId(), o.getPaymentId(), o.getPaymentKind(), o.getPaymentAmt()))
                .collect(Collectors.toList());
        updatedReportEvent.setPaymentDetails(newDetails);

        AggregateLifecycle.apply(updatedReportEvent);
    }

    @CommandHandler
    private void handle(UpdateReportDeliveryStatusCommand cmd) {
        log.info("[@CommandHandler] Handle <UpdateReportDeliveryStatusCommand> for Order Id: {}", cmd.getOrderId());
        UpdatedReportDeliveryStatusEvent event = new UpdatedReportDeliveryStatusEvent();
        BeanUtils.copyProperties(cmd, event);
        apply(event);
    }
}
