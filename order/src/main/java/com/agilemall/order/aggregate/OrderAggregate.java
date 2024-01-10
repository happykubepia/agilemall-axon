package com.agilemall.order.aggregate;

/*
- Class: OrderAggregate
- Object: 내/외부로 부터의 요청(Command)을 받는 Aggregate
- Created: 2024-01-01
- Updated: 2024-01-08
- Owner: 갑빠(hiondal)
*/

import com.agilemall.common.command.create.CancelCreateOrderCommand;
import com.agilemall.common.events.create.CancelledCreateOrderEvent;
import com.agilemall.common.events.create.CompletedCreateOrderEvent;
import com.agilemall.common.events.update.CancelledUpdateOrderEvent;
import com.agilemall.common.events.update.CompletedUpdateOrderEvent;
import com.agilemall.order.command.*;
import com.agilemall.order.command.CompleteUpdateOrderCommand;
import com.agilemall.common.dto.OrderDTO;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.entity.OrderDetailIdentity;
import com.agilemall.order.events.CreatedOrderEvent;
import com.agilemall.order.events.UpdatedOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
//@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache="snapshotCache")
@Aggregate
public class OrderAggregate {
    @AggregateIdentifier
    private String orderId;
    @AggregateMember
    private String userId;
    @AggregateMember
    private LocalDateTime orderDatetime;
    @AggregateMember
    private String orderStatus;
    @AggregateMember
    private int totalOrderAmt;
    @AggregateMember
    private List<OrderDetail> orderDetails;

    private List<OrderDTO> aggregateHistory = new ArrayList<>();

    @Autowired
    private transient CommandGateway commandGateway;

    public OrderAggregate() {

    }

    /*
    - Object: 새로운 Aggregate 객체를 생성
    - Description:
    */
    @CommandHandler
    private OrderAggregate(CreateOrderCommand createOrderCommand) {
        log.info("[@CommandHandler] Executing <CreateOrderCommand> for Order Id: {}", createOrderCommand.getOrderId());

        CreatedOrderEvent createdOrderEvent = new CreatedOrderEvent();
        createdOrderEvent.setOrderId(createOrderCommand.getOrderId());
        createdOrderEvent.setUserId(createOrderCommand.getUserId());
        createdOrderEvent.setOrderDatetime(createOrderCommand.getOrderDatetime());
        createdOrderEvent.setOrderStatus(createOrderCommand.getOrderStatus());
        createdOrderEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        createdOrderEvent.setOrderDetails(createOrderCommand.getOrderDetails());
        createdOrderEvent.setPaymentId(createOrderCommand.getPaymentId());
        createdOrderEvent.setPaymentDetails(createOrderCommand.getPaymentDetails());
        createdOrderEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        createdOrderEvent.setTotalPaymentAmt(createOrderCommand.getTotalPaymentAmt());

        AggregateLifecycle.apply(createdOrderEvent);
    }

    @EventSourcingHandler
    private void on(CreatedOrderEvent createdOrderEvent) {
        log.info("[@EventSourcingHandler] Executing <CreatedOrderEvent> for Order Id: {}", createdOrderEvent.getOrderId());

        this.orderId = createdOrderEvent.getOrderId();
        this.userId = createdOrderEvent.getUserId();
        this.orderDatetime = createdOrderEvent.getOrderDatetime();
        this.orderStatus = createdOrderEvent.getOrderStatus();
        this.orderDetails = createdOrderEvent.getOrderDetails().stream()
                .map(o -> new OrderDetail((new OrderDetailIdentity(this.orderId, o.getProductId())), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        this.totalOrderAmt = createdOrderEvent.getTotalOrderAmt();
    }

    @CommandHandler
    private void handle(CompleteOrderCommand completeOrderCommand) throws RuntimeException {
        log.info("[@CommandHandler] Executing <CompleteOrderCommand> for Order Id: {}", completeOrderCommand.getOrderId());

        if("".equals(completeOrderCommand.getOrderId())) {
            throw new RuntimeException("Order Id is MUST NULL");
        }
        CompletedCreateOrderEvent completedCreateOrderEvent = new CompletedCreateOrderEvent();
        BeanUtils.copyProperties(completeOrderCommand, completedCreateOrderEvent);

        AggregateLifecycle.apply(completedCreateOrderEvent);
    }

    @EventSourcingHandler
    private void on(CompletedCreateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CompletedCreateOrderEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();
    }

    @CommandHandler
    private void handle(CancelCreateOrderCommand cancelCreateOrderCommand) {
        log.info("[@CommandHandler] Executing <CancelCreateOrderCommand> for Order Id: {}", cancelCreateOrderCommand.getOrderId());

        CancelledCreateOrderEvent cancelledCreateOrderEvent = new CancelledCreateOrderEvent();
        BeanUtils.copyProperties(cancelCreateOrderCommand, cancelledCreateOrderEvent);

        AggregateLifecycle.apply(cancelledCreateOrderEvent);
    }

    @EventSourcingHandler
    private void on(CancelledCreateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledCreateOrderEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();
    }

    @CommandHandler
    private void handle(UpdateOrderCommand updateOrderCommand) {
        log.info("[@CommandHandler] Executing <UpdateOrderCommand> for Order Id: {}", updateOrderCommand.getOrderId());

        UpdatedOrderEvent updatedOrderEvent = new UpdatedOrderEvent();
        BeanUtils.copyProperties(updateOrderCommand, updatedOrderEvent);

        AggregateLifecycle.apply(updatedOrderEvent);
    }

    @EventSourcingHandler
    private void on(UpdatedOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <UpdatedOrderEvent> for Order Id: {}", event.getOrderId());

        this.aggregateHistory.add(cloneAggregate(this));    //보상처리를 위해 이전 정보 저장

        this.orderDatetime = event.getOrderDatetime();
        this.orderStatus = OrderStatusEnum.UPTATED.value();
        this.totalOrderAmt = event.getTotalOrderAmt();
        List<OrderDetail> orderDetails = this.orderDetails.stream().toList();
        this.orderDetails.clear();
        for(OrderDetail item:orderDetails) {
            Optional<OrderDetailDTO> optDetail = event.getOrderDetails().stream()
                    .filter(o -> o.getProductId().equals(item.getOrderDetailIdentity().getProductId()))
                    .findFirst();
            if(optDetail.isPresent()) {
                item.setQty(optDetail.get().getQty());
                item.setOrderAmt(optDetail.get().getOrderAmt());
            }
            this.orderDetails.add(item);
        }
    }

    @CommandHandler
    private void on(CompleteUpdateOrderCommand cmd) {
        log.info("[@CommandHandler] Executing <CompleteUpdateOrderCommand> for Order Id: {}", cmd.getOrderId());

        CompletedUpdateOrderEvent event = new CompletedUpdateOrderEvent();
        event.setOrderId(cmd.getOrderId());
        event.setOrderStatus(cmd.getOrderStatus());

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    private void handle(CompletedUpdateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CompletedUpdateOrderEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();

    }

    @CommandHandler
    private void handle(CancelUpdateOrderCommand cancelUpdateOrderCommand) {
        log.info("[@CommandHandler] Executing <CancelUpdateOrderCommand> for Order Id: {}", cancelUpdateOrderCommand.getOrderId());

        CancelledUpdateOrderEvent cancelledUpdateOrderEvent = new CancelledUpdateOrderEvent();
        cancelledUpdateOrderEvent.setOrderId(cancelUpdateOrderCommand.getOrderId());

        AggregateLifecycle.apply(cancelledUpdateOrderEvent);

        //-- send UpdateOrderCommand to compensate
        if(this.aggregateHistory.isEmpty()) return;

        OrderDTO order = this.aggregateHistory.get(this.aggregateHistory.size() - 1);
        UpdateOrderCommand cmd = UpdateOrderCommand.builder()
                .orderId(order.getOrderId())
                .orderDatetime(order.getOrderDatetime())
                .totalOrderAmt(order.getTotalOrderAmt())
                .orderDetails(order.getOrderDetails())
                .isCompensation(cancelUpdateOrderCommand.isCompensation())           //보상처리 Command 표시
                .build();

        commandGateway.send(cmd);
    }

    @EventSourcingHandler
    private void on(CancelledUpdateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledUpdateOrderEvent> for Order Id: {}", event.getOrderId());

    }

    private OrderDTO cloneAggregate(OrderAggregate orderAggregate) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderAggregate.orderId);
        order.setUserId(orderAggregate.userId);
        order.setOrderDatetime(orderAggregate.orderDatetime);
        order.setOrderStatus(orderAggregate.orderStatus);
        order.setOrderDetails(
                orderAggregate.orderDetails.stream()
                        .map(o -> new OrderDetailDTO(
                                order.getOrderId(), o.getOrderDetailIdentity().getProductId(),
                                o.getQty(), o.getOrderAmt()))
                        .collect(Collectors.toList()));
        return order;
    }
}
