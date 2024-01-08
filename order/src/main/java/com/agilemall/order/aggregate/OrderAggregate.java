package com.agilemall.order.aggregate;

import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.command.CompleteOrderCommand;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.OrderCancelledEvent;
import com.agilemall.common.events.OrderCompletedEvent;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.order.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Aggregate
public class OrderAggregate {
    @AggregateIdentifier
    private String orderId;
    private String userId;
    private LocalDateTime orderDatetime;
    private String orderStatus;
    private int totalOrderAmt;
    private List<OrderDetailDTO> orderDetails;
    private String paymentId;
    private List<PaymentDetailDTO> paymentDetails;
    private int totalPaymentAmt;

    public OrderAggregate() {

    }

    @CommandHandler
    public OrderAggregate(CreateOrderCommand createOrderCommand) {
        log.info("[@CommandHandler] Executing <CreateOrderCommand> for Order Id: {}", createOrderCommand.getOrderId());

        OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent();
        orderCreatedEvent.setOrderId(createOrderCommand.getOrderId());
        orderCreatedEvent.setUserId(createOrderCommand.getUserId());
        orderCreatedEvent.setOrderDatetime(createOrderCommand.getOrderDatetime());
        orderCreatedEvent.setOrderStatus(createOrderCommand.getOrderStatus());
        orderCreatedEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        orderCreatedEvent.setOrderDetails(createOrderCommand.getOrderDetails());
        orderCreatedEvent.setPaymentId(createOrderCommand.getPaymentId());
        orderCreatedEvent.setPaymentDetails(createOrderCommand.getPaymentDetails());
        orderCreatedEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        orderCreatedEvent.setTotalPaymentAmt(createOrderCommand.getTotalPaymentAmt());

        AggregateLifecycle.apply(orderCreatedEvent);
    }

    @EventSourcingHandler
    public void on(OrderCreatedEvent orderCreatedEvent) {
        log.info("[@EventSourcingHandler] Executing <OrderCreatedEvent> for Order Id: {}", orderCreatedEvent.getOrderId());

        this.orderId = orderCreatedEvent.getOrderId();
        this.userId = orderCreatedEvent.getUserId();
        this.orderDatetime = orderCreatedEvent.getOrderDatetime();
        this.orderStatus = orderCreatedEvent.getOrderStatus();
        this.orderDetails = orderCreatedEvent.getOrderDetails();
        this.paymentId = orderCreatedEvent.getPaymentId();
        this.paymentDetails = orderCreatedEvent.getPaymentDetails();
        this.totalOrderAmt = orderCreatedEvent.getTotalOrderAmt();

    }

    @CommandHandler
    public void handle(CompleteOrderCommand completeOrderCommand) throws RuntimeException {
        log.info("[@CommandHandler] Executing <CompleteOrderCommand> for Order Id: {}", completeOrderCommand.getOrderId());

        if("".equals(completeOrderCommand.getOrderId())) {
            throw new RuntimeException("Order Id is MUST NULL");
        }
        OrderCompletedEvent orderCompletedEvent = new OrderCompletedEvent();
        BeanUtils.copyProperties(completeOrderCommand, orderCompletedEvent);

        AggregateLifecycle.apply(orderCompletedEvent);
    }

    @EventSourcingHandler
    public void on(OrderCompletedEvent event) {
        log.info("[@EventSourcingHandler] Executing <OrderCompletedEvent> for Order Id: {}", event.getOrderId());
        //log.info("Order Status is {}", event.getOrderStatus());
        this.orderStatus = event.getOrderStatus();
    }

    @CommandHandler
    public void handle(CancelOrderCommand cancelOrderCommand) {
        log.info("[@CommandHandler] Executing <CancelOrderCommand> for Order Id: {}", cancelOrderCommand.getOrderId());

        OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent();
        BeanUtils.copyProperties(cancelOrderCommand, orderCancelledEvent);

        AggregateLifecycle.apply(orderCancelledEvent);
    }

    @EventSourcingHandler
    public void on(OrderCancelledEvent event) {
        log.info("[@EventSourcingHandler] Executing <OrderCancelledEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();
    }
}
