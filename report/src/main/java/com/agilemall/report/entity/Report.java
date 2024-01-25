package com.agilemall.report.entity;
/*
- 목적: Table과 매핑되는 Entity와 Command Handler인 Aggregate 정의
- 설명
    - Event replay로 최종 상태를 계산하는 일반 Aggregate가 아닌 DB에 최종 상태를 저장하는 State stored Aggregate를 정의
    - Report는 Order, Payment, Delivery의 데이터를 사용하여 데이터를 재생성할 수 있으모로 Event sourcing 패턴 미적용
*/

import com.agilemall.common.command.create.CreateReportCommand;
import com.agilemall.common.command.delete.DeleteReportCommand;
import com.agilemall.common.command.update.UpdateReportCommand;
import com.agilemall.common.command.update.UpdateReportDeliveryStatusCommand;
import com.agilemall.common.events.delete.DeletedReportEvent;
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

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Slf4j
@Aggregate  //State Stored Aggregate(최종상태를 Event Replay가 아닌 DB를 이용하는 Aggregate)는 Entity에 정의
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
    @Column(name = "order_details", length = 3000)
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
    @Column(name = "payment_details", length = 3000)
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
    }


    @CommandHandler
    private void handle(UpdateReportDeliveryStatusCommand cmd) {
        log.info("[@CommandHandler] Handle <UpdateReportDeliveryStatusCommand> for Order Id: {}", cmd.getOrderId());

        this.deliveryStatus = cmd.getDeliveryStatus();
    }

    @CommandHandler
    private void handle(DeleteReportCommand deleteReportCommand) {
        log.info("[@CommandHandler] Handle <DeleteReportCommand> for Order Id: {}", deleteReportCommand.getOrderId());

        AggregateLifecycle.apply(new DeletedReportEvent(deleteReportCommand.getReportId(), deleteReportCommand.getOrderId()));
        //AggregateLifecycle.markDeleted();
    }
}
