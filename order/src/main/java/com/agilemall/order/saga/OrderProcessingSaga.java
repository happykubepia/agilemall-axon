package com.agilemall.order.saga;

import com.agilemall.common.command.CompleteOrderCommand;
import com.agilemall.common.command.CreatePaymentCommand;
import com.agilemall.common.command.CreateReportCommand;
import com.agilemall.common.command.CreateDeliveryCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.*;
import com.agilemall.common.events.OrderCancelledEvent;
import com.agilemall.common.events.OrderCompletedEvent;
import com.agilemall.common.events.DeliveryCreatedEvent;
import com.agilemall.common.events.PaymentCreatedEvent;
import com.agilemall.order.events.OrderCreatedEvent;
import com.agilemall.order.service.CompensatingService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderProcessingSaga {
    @Autowired
    private transient CommandGateway commandGateway;

    @Autowired
    private CompensatingService compensatingService;

    @Autowired
    private transient  QueryGateway queryGateway;

    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void handle(OrderCreatedEvent event) {
        log.info("[Saga] OrderCreatedEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #3: <CreatePaymentCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), event.getOrderId());

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
               compensatingService.cancelOrder(aggregateIdMap);
           }
        });

        //commandGateway.sendAndWait(createPaymentCommand);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void handle(PaymentCreatedEvent event) {
        log.info("[Saga] [CreatePaymentCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #4: <DeliveryOrderCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), event.getPaymentId());

        CreateDeliveryCommand createDeliveryCommand = CreateDeliveryCommand.builder()
                .deliveryId("SHIP_"+RandomStringUtils.random(10, false, true))
                .orderId(event.getOrderId())
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .aggregateIdMap(aggregateIdMap)
                .build();

        //commandGateway.sendAndWait(deliveryOrderCommand);

        commandGateway.send(createDeliveryCommand, (commandMessage, resultMessage) -> {
            if(resultMessage.isExceptional()) {
                log.info("Error is occurred during handle <deliveryOrderCommand>: {}", String.valueOf(resultMessage.exceptionResult()));
                compensatingService.cancelPayment(aggregateIdMap);

                //do compensating transaction: Order
                compensatingService.cancelOrder(aggregateIdMap);
            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    public void handle(DeliveryCreatedEvent event) {
        log.info("[Saga] [DeliveryOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction #5: <CompleteOrderCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.DELIVERY.value(), event.getDeliveryId());
        CompleteOrderCommand completeOrderCommand = CompleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatusEnum.APPROVED.value())
                .aggregateIdMap(aggregateIdMap)
                .build();

        //commandGateway.sendAndWait(completeOrderCommand);

        commandGateway.send(completeOrderCommand, (commandMessage, resultMessage) -> {
            if(resultMessage.isExceptional()) {
                log.info("Error is occurred during handle <CompletedOrderCommand>: {}",String.valueOf(resultMessage.exceptionResult()));

                //-- request compensating transaction
                compensatingService.cancelDelivery(aggregateIdMap);

                //do compensating transaction: Payment
                compensatingService.cancelPayment(aggregateIdMap);

                //do compensating transaction: Order
                compensatingService.cancelOrder(aggregateIdMap);

            }
        });
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCompletedEvent event) {
        log.info("[Saga] [CompleteOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Transaction FINISHED =====");

        //-- Report service에 신규 레포트 record 생성
        log.info("===== [Saga] START generate report =====");

        try {
            OrderDTO order = queryGateway.query(Constants.QUERY_REPORT, event.getOrderId(),
                    ResponseTypes.instanceOf(OrderDTO.class)).join();
            PaymentDTO payment = queryGateway.query(Constants.QUERY_REPORT, event.getOrderId(),
                    ResponseTypes.instanceOf(PaymentDTO.class)).join();
            DeliveryDTO delivery = queryGateway.query(Constants.QUERY_REPORT, event.getOrderId(),
                    ResponseTypes.instanceOf(DeliveryDTO.class)).join();

            CreateReportCommand createReportCommand = CreateReportCommand.builder()
                    .reportId(RandomStringUtils.random(15,false, true))
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .orderDatetime(order.getOrderDatetime())
                    .totalOrderAmt(order.getTotalOrderAmt())
                    .orderStatus(order.getOrderStatus())
                    .orderDetails(order.getOrderDetails())
                    .paymentId(payment.getPaymentId())
                    .totalPaymentAmt(payment.getTotalPaymentAmt())
                    .paymentStatus(payment.getPaymentStatus())
                    .paymentDetails(payment.getPaymentDetails())
                    .deliveryId(delivery.getDeliveryId())
                    .deliveryStatus(delivery.getDeliveryStatus())
                    .build();
            commandGateway.sendAndWait(createReportCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            log.info("===== [Saga] END generate report =====");
        } catch(Exception e) {
            log.info(e.getMessage());
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    public void handle(OrderCancelledEvent event) {
        log.info("[Saga] OrderCancelledEvent in Saga for Order Id: {}", event.getOrderId());
    }
}
