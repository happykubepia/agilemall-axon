package com.agilemall.order.saga;

import com.agilemall.common.command.CompleteOrderCommand;
import com.agilemall.common.command.CreatePaymentCommand;
import com.agilemall.common.command.DeliveryOrderCommand;
import com.agilemall.common.dto.OrderStatus;
import com.agilemall.common.dto.ServiceName;
import com.agilemall.common.events.OrderCancelledEvent;
import com.agilemall.common.events.OrderCompletedEvent;
import com.agilemall.common.events.OrderDeliveredEvent;
import com.agilemall.common.events.PaymentProcessedEvent;
import com.agilemall.order.events.OrderCreatedEvent;
import com.agilemall.order.service.CompensatingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;

@Saga
@Slf4j
public class OrderProcessingSaga {
    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private CompensatingService compensatingService;

    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void handle(OrderCreatedEvent event) {
        log.info("[Saga] OrderCreatedEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #3: <CreatePaymentCommand> =====");

        aggregateIdMap.put(ServiceName.ORDER.value(), event.getOrderId());

        //결제 처리
        CreatePaymentCommand createPaymentCommand = CreatePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .totalPaymentAmt(event.getTotalPaymentAmt())
                .paymentDetails(event.getPaymentDetails())
                .aggregateIdMap(aggregateIdMap)
                .build();

        commandGateway.send(createPaymentCommand, (commandMessage, resultMessage) -> {
           if(resultMessage.isExceptional()) {
               log.info("Error is occurred during handle <createPaymentCommand>: {}", String.valueOf(resultMessage.exceptionResult()));
               compensatingService.cancelOrderCommand(aggregateIdMap);
           }
        });

        //commandGateway.sendAndWait(createPaymentCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void handle(PaymentProcessedEvent event) {
        log.info("[Saga] [CreatePaymentCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #4: <DeliveryOrderCommand> =====");

        aggregateIdMap.put(ServiceName.PAYMENT.value(), event.getPaymentId());

        DeliveryOrderCommand deliveryOrderCommand = DeliveryOrderCommand.builder()
                .deliveryId("SHIP_"+RandomStringUtils.random(10, false, true))
                .orderId(event.getOrderId())
                .aggregateIdMap(aggregateIdMap)
                .build();

        //commandGateway.sendAndWait(deliveryOrderCommand);

        commandGateway.send(deliveryOrderCommand, (commandMessage, resultMessage) -> {
            if(resultMessage.isExceptional()) {
                log.info("Error is occurred during handle <deliveryOrderCommand>: {}", String.valueOf(resultMessage.exceptionResult()));
                compensatingService.cancelPaymentCommand(aggregateIdMap);

                //do compensating transaction: Order
                compensatingService.cancelOrderCommand(aggregateIdMap);
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(OrderDeliveredEvent event) {
        log.info("[Saga] [DeliveryOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #5: <CompleteOrderCommand> =====");

        aggregateIdMap.put(ServiceName.DELIVERY.value(), event.getDeliveryId());
        CompleteOrderCommand completeOrderCommand = CompleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatus.APPROVED.value())
                .aggregateIdMap(aggregateIdMap)
                .build();

        //commandGateway.sendAndWait(completeOrderCommand);

        commandGateway.send(completeOrderCommand, (commandMessage, resultMessage) -> {
            if(resultMessage.isExceptional()) {
                log.info("Error is occurred during handle <CompletedOrderCommand>: {}",String.valueOf(resultMessage.exceptionResult()));

                //-- request compensating transaction
                compensatingService.cancelDeliveryCommand(aggregateIdMap);

                //do compensating transaction: Payment
                compensatingService.cancelPaymentCommand(aggregateIdMap);

                //do compensating transaction: Order
                compensatingService.cancelOrderCommand(aggregateIdMap);

            }
        });


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
