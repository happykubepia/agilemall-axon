package com.agilemall.order.saga;

import com.agilemall.common.command.create.CreateDeliveryCommand;
import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.create.*;
import com.agilemall.order.command.CompleteOrderCommand;
import com.agilemall.order.events.CreatedOrderEvent;
import com.agilemall.order.events.FailedCompleteCreateOrderEvent;
import com.agilemall.order.events.FailedCreateOrderEvent;
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
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderCreatingSaga {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private CompensatingService compensatingService;

    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CreatedOrderEvent event) {
        log.info("[Saga] CreatedOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Creating Order Transaction #3: <CreatePaymentCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), event.getOrderId());

        //결제 처리
        CreatePaymentCommand createPaymentCommand = CreatePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .totalPaymentAmt(event.getTotalPaymentAmt())
                .paymentDetails(event.getPaymentDetails())
                .build();

        try {
            commandGateway.sendAndWait(createPaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info("Error is occurred during handle <createPaymentCommand>: {}", e.getMessage());
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(CreatedPaymentEvent event) {
        log.info("[Saga] [CreatePaymentCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Creating Order Transaction #4: <DeliveryOrderCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), event.getPaymentId());

        CreateDeliveryCommand createDeliveryCommand = CreateDeliveryCommand.builder()
                .deliveryId("SHIP_"+RandomStringUtils.random(10, false, true))
                .orderId(event.getOrderId())
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .build();

        try {
            commandGateway.sendAndWait(createDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info("Error is occurred during handle <deliveryOrderCommand>: {}", e.getMessage());
            compensatingService.cancelCreatePayment(aggregateIdMap);
            //do compensating transaction: Order
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(CreatedDeliveryEvent event) {
        log.info("[Saga] [DeliveryOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Creating Order Transaction #5: <CompleteOrderCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.DELIVERY.value(), event.getDeliveryId());
        CompleteOrderCommand completeOrderCommand = CompleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatusEnum.COMPLETED.value())
                .build();

        try {
            commandGateway.sendAndWait(completeOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info("Error is occurred during handle <CompletedOrderCommand>: {}", e.getMessage());

            //-- request compensating transaction
            compensatingService.cancelCreateDelivery(aggregateIdMap);
            //do compensating transaction: Payment
            compensatingService.cancelCreatePayment(aggregateIdMap);
            //do compensating transaction: Order
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    private void on(CompletedCreateOrderEvent event) {
        log.info("[Saga] [CompleteOrderCommand] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Creating Order Transaction FINISHED =====");

        //-- Report service에 신규 레포트 record 생성
        compensatingService.updateReport(event.getOrderId(), true);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateOrderEvent event) {
        log.info("[Saga] Handle <FailedCreateOrderEvent> for Order Id: {}", event.getOrderId());

        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreatePaymentEvent event) {
        log.info("[Saga] Handle <FailedCreatePaymentEvent> for Order Id: {}", event.getOrderId());

        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateDeliveryEvent event) {
        log.info("[Saga] Handle <FailedCreateDeliveryEvent> for Order Id: {}", event.getOrderId());

        compensatingService.cancelCreatePayment(this.aggregateIdMap);
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCompleteCreateOrderEvent event) {
        log.info("[Saga] Handle <FailedCompleteCreateOrderEvent> for Order Id: {}", event.getOrderId());

        compensatingService.cancelCreatePayment(this.aggregateIdMap);
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
        compensatingService.cancelCreateDelivery(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    @EndSaga
    private void on(CancelledCreateOrderEvent event) {
        log.info("[Saga] CancelledCreateOrderEvent in Saga for Order Id: {}", event.getOrderId());
        log.info("===== [Saga] Creating Order Transaction Aborted =====");

    }

}
