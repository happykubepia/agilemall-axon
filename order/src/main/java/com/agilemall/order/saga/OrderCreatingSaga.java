package com.agilemall.order.saga;

import com.agilemall.common.command.create.CreateDeliveryCommand;
import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.create.*;
import com.agilemall.order.command.CompleteOrderCreateCommand;
import com.agilemall.order.events.*;
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
        log.info("[Saga] <CreatedOrderEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] #3: <CreatePaymentCommand> =====");

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
            log.error("Error is occurred during handle <CreatePaymentCommand>: {}", e.getMessage());
            log.info("===== [Create Order] Compensate <CancelCreateOrderCommand>");
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(CreatedPaymentEvent event) {
        log.info("[Saga] <CreatedPaymentEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] #4: <CreateDeliveryCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), event.getPaymentId());

        CreateDeliveryCommand createDeliveryCommand = CreateDeliveryCommand.builder()
                .deliveryId("SHIP_"+RandomStringUtils.random(10, false, true))
                .orderId(event.getOrderId())
                .deliveryStatus(DeliveryStatusEnum.CREATED.value())
                .build();

        try {
            commandGateway.sendAndWait(createDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info("Error is occurred during handle <CreateDeliveryCommand>: {}", e.getMessage());
            log.info("===== [Create Order] Compensate <CancelCreatePaymentCommand> ====");
            compensatingService.cancelCreatePayment(aggregateIdMap);
            log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(CreatedDeliveryEvent event) {
        log.info("[Saga] <CreatedDeliveryEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] #5: <CompleteOrderCreateCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.DELIVERY.value(), event.getDeliveryId());
        CompleteOrderCreateCommand completeOrderCreateCommand = CompleteOrderCreateCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatusEnum.COMPLETED.value())
                .build();

        try {
            commandGateway.sendAndWait(completeOrderCreateCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info("Error is occurred during handle <CompleteOrderCreateCommand>: {}", e.getMessage());

            log.info("===== [Create Order] Compensate <CancelCreateDeliveryCommand> ====");
            compensatingService.cancelCreateDelivery(aggregateIdMap);
            log.info("===== [Create Order] Compensate <CancelCreatePaymentCommand> ====");
            compensatingService.cancelCreatePayment(aggregateIdMap);
            log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateOrderEvent event) {
        log.info("[Saga] <FailedCreateOrderEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreatePaymentEvent event) {
        log.info("[Saga] <FailedCreatePaymentEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateDeliveryEvent event) {
        log.info("[Saga] Handle <FailedCreateDeliveryEvent> for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreatePaymentCommand> ====");
        compensatingService.cancelCreatePayment(this.aggregateIdMap);
        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCompleteCreateOrderEvent event) {
        log.info("[Saga] Handle <FailedCompleteCreateOrderEvent> for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreateDeliveryCommand> ====");
        compensatingService.cancelCreateDelivery(this.aggregateIdMap);
        log.info("===== [Create Order] Compensate <CancelCreatePaymentCommand> ====");
        compensatingService.cancelCreatePayment(this.aggregateIdMap);
        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);

    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedCreateOrderEvent event) {
        log.info("[Saga] [CompletedCreateOrderEvent] is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] Transaction is FINISHED =====");

        //-- Report service에 레포트 생성 요청
        compensatingService.updateReport(event.getOrderId(), true);
    }

    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CancelledCreateOrderEvent event) {
        log.info("[Saga] CancelledCreateOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] Transaction is Aborted =====");
    }
}
