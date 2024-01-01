package com.agilemall.order.saga;

import com.agilemall.common.command.*;
import com.agilemall.common.events.*;
import com.agilemall.order.events.OrderCreatedEvent;
import com.agilemall.order.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

@Saga
@Slf4j
public class OrderProcessingSaga {
    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private InventoryService inventoryService;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void handle(OrderCreatedEvent event) {
        log.info("[Saga] OrderCreatedEvent in Saga for Order Id: {}", event.getOrderId());
        boolean isValidInventory;
        try {
            isValidInventory = inventoryService.isValidInventory(event);

            if(isValidInventory) {
                //결제 처리
                CreatePaymentCommand createPaymentCommand = CreatePaymentCommand.builder()
                        .paymentId(event.getPaymentId())
                        .orderId(event.getOrderId())
                        .totalPaymentAmt(event.getTotalPaymentAmt())
                        .paymentDetails(event.getPaymentDetails())
                        .build();
                commandGateway.sendAndWait(createPaymentCommand);
            } else {
                cancelOrderCommand(event.getOrderId());
            }
        } catch(Exception e) {
            log.error(e.getMessage());
            cancelOrderCommand(event.getOrderId());
        }
    }

    private void cancelOrderCommand(String orderId) {
        CancelOrderCommand cancelOrderCommand = new CancelOrderCommand(orderId);
        commandGateway.sendAndWait(cancelOrderCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void handle(PaymentProcessedEvent event) {
        log.info("[Saga] PaymentProcessedEvent in Saga for Order Id: {}", event.getOrderId());
        try {
            DeliveryOrderCommand deliveryOrderCommand = DeliveryOrderCommand.builder()
                    .deliveryId(RandomStringUtils.random(15, false, true))
                    .orderId(event.getOrderId())
                    .build();
            commandGateway.sendAndWait(deliveryOrderCommand);

        } catch(Exception e) {
            log.error(e.getMessage());
            cancelPaymentCommand(event);
        }
    }
    private void cancelPaymentCommand(PaymentProcessedEvent event) {
        CancelPaymentCommand cancelPaymentCommand = new CancelPaymentCommand(event.getPaymentId(), event.getOrderId());
        commandGateway.sendAndWait(cancelPaymentCommand);

    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderDeliveriedEvent event) {
        log.info("[Saga] OrderDeliveriedEvent in Saga for Order Id: {}", event.getOrderId());

        CompleteOrderCommand completeOrderCommand = CompleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus("APPROVED")
                .build();

        commandGateway.sendAndWait(completeOrderCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCompletedEvent event) {
        log.info("[Saga] OrderCompletedEvent in Saga for Order Id: {}", event.getOrderId());

    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCancelledEvent event) {
        log.info("[Saga] OrderCancelledEvent in Saga for Order Id: {}", event.getOrderId());
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(PaymentCancelledEvent event) {
        log.info("[Saga] PaymentCancelledEvent in Saga for Order Id: {}", event.getOrderId());
        cancelOrderCommand(event.getOrderId());
    }

}
