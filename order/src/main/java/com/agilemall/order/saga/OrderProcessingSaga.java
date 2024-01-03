package com.agilemall.order.saga;

import com.agilemall.common.command.*;
import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import com.agilemall.common.dto.InventoryQtyAdjustType;
import com.agilemall.common.events.*;
import com.agilemall.order.dto.OrderDetailDTO;
import com.agilemall.order.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Saga
@Slf4j
public class OrderProcessingSaga {
    @Autowired
    private transient CommandGateway commandGateway;

    private List<OrderDetailDTO> orderDetails;

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void handle(OrderCreatedEvent event) {
        log.info("[Saga] OrderCreatedEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #3: <CreatePaymentCommand> =====");

        orderDetails = event.getOrderDetails();

        try {
            //결제 처리
            CreatePaymentCommand createPaymentCommand = CreatePaymentCommand.builder()
                    .paymentId(event.getPaymentId())
                    .orderId(event.getOrderId())
                    .totalPaymentAmt(event.getTotalPaymentAmt())
                    .paymentDetails(event.getPaymentDetails())
                    .build();
            commandGateway.sendAndWait(createPaymentCommand);
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
        log.info("[Saga] [CreatePaymentCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #4: <DeliveryOrderCommand> =====");
        try {
            DeliveryOrderCommand deliveryOrderCommand = DeliveryOrderCommand.builder()
                    .deliveryId("SHIP_"+RandomStringUtils.random(10, false, true))
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
        log.info("[Saga] [DeliveryOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #5: <InventoryQtyAdjustCommand> =====");
        try {
            List<InventoryQtyAdjustDTO> adjustQtyList = new ArrayList<>();
            InventoryQtyAdjustDTO adjustQty;

            for(OrderDetailDTO order:orderDetails) {
                adjustQty = InventoryQtyAdjustDTO.builder()
                        .productId(order.getProductId())
                        .adjustType(InventoryQtyAdjustType.DECREASE.value())
                        .adjustQty(order.getQty())
                        .build();
                //log.info("adjustQty: {}", adjustQty.toString());
                adjustQtyList.add(adjustQty);
            }

            InventoryQtyAdjustCommand inventoryQtyAdjustCommand = InventoryQtyAdjustCommand.builder()
                    .inventoryId(RandomStringUtils.random(15, false, true))
                    .orderId(event.getOrderId())
                    .inventoryQtyAdjustDetails(adjustQtyList)
                    .build();

            commandGateway.sendAndWait(inventoryQtyAdjustCommand);

        } catch(Exception e) {
            log.error(e.getMessage());
            cancelDeliveryCommand(event);
        }
    }


    private void cancelDeliveryCommand(OrderDeliveriedEvent event) {
        CancelDeliveryCommand cancelDeliveryCommand = new CancelDeliveryCommand(event.getDeliveryId(), event.getOrderId());
        commandGateway.sendAndWait(cancelDeliveryCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(InventoryQtyAdjustedEvent event) {
        log.info("[Saga] [InventoryQtyAdjustCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #6: <CompleteOrderCommand> =====");

        try {
            CompleteOrderCommand completeOrderCommand = CompleteOrderCommand.builder()
                    .orderId(event.getOrderId())
                    .orderStatus("APPROVED")
                    .build();

            commandGateway.sendAndWait(completeOrderCommand);
        } catch(Exception e) {
            log.error(e.getMessage());
            cancelInventoryQtyAdjustCommand(event);
        }

    }
    private void cancelInventoryQtyAdjustCommand(InventoryQtyAdjustedEvent event) {
        List<InventoryQtyAdjustDTO> adjustQtyList = new ArrayList<>();
        InventoryQtyAdjustDTO adjustQty;

        for(OrderDetailDTO order:orderDetails) {
            adjustQty = InventoryQtyAdjustDTO.builder()
                    .productId(order.getProductId())
                    .adjustType(InventoryQtyAdjustType.INCREASE.value())
                    .adjustQty(order.getQty())
                    .build();

            adjustQtyList.add(adjustQty);
        }

        InventoryQtyAdjustCommand inventoryQtyAdjustCommand = InventoryQtyAdjustCommand.builder()
                .inventoryId(event.getInventoryId())
                .orderId(event.getOrderId())
                .inventoryQtyAdjustDetails(adjustQtyList)
                .build();

        commandGateway.sendAndWait(inventoryQtyAdjustCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCompletedEvent event) {
        log.info("[Saga] [CompleteOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction FINISHED =====");
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCancelledEvent event) {
        log.info("[Saga] OrderCancelledEvent in Saga for Order Id: {}", event.getOrderId());
    }

}
