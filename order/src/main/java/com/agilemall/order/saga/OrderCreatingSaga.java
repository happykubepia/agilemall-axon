package com.agilemall.order.saga;
/*
- 목적: 신규 주문 처리를 위한 Saga 프로세스 처리
- 설명
    - 정상 처리: CreatedOrderEvent -> CreatedPaymentEvent ->  CreatedDeliveryEvent -> CompletedCreateOrderEvent
    - 실패 처리: 보상처리(Compensating tranaction) 수행
      - FailedCreateOrderEvent: compensatingService.cancelCreateOrder
      - FailedCreatePaymentEvent: compensatingService.cancelCreateOrder
      - FailedCreateDeliveryEvent: compensatingService.cancelCreatePayment -> compensatingService.cancelCreateOrder
      - FailedCompleteCreateOrderEvent: compensatingService.cancelCreateDelivery -> compensatingService.cancelCreatePayment -> compensatingService.cancelCreateOrder
    - 정상 처리 완료 시 compensatingService.updateReportReport 호출하여 Report service에 조회용 데이터 생성
*/

import com.agilemall.common.command.create.CreateDeliveryCommand;
import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.create.CreatedDeliveryEvent;
import com.agilemall.common.events.create.CreatedPaymentEvent;
import com.agilemall.common.events.create.FailedCreateDeliveryEvent;
import com.agilemall.common.events.create.FailedCreatePaymentEvent;
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
    //-- 주문ID, 결제ID, 배송ID값을 담은 변수
    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

    private transient CommandGateway commandGateway;
    @Autowired
    public void setCommandGateway(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    private CompensatingService compensatingService;
    @Autowired
    public void setCompensatingService(CompensatingService compensatingService) {
        this.compensatingService = compensatingService;
    }

    //================== 정상 처리 프로세스 ====================

    //-- 결제정보 생성 요청
    @StartSaga      //Saga 프로세스 시작을 나타냄
    @SagaEventHandler(associationProperty = "orderId")  //Event 메시지의 Unique한 구별자
    private void on(CreatedOrderEvent event) {
        log.info("[Saga] <CreatedOrderEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] #3: <CreatePaymentCommand> =====");

        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), event.getOrderId());

        //결제 처리 요청 Command메시지 생성
        CreatePaymentCommand createPaymentCommand = CreatePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .totalPaymentAmt(event.getTotalPaymentAmt())
                .paymentDetails(event.getPaymentDetails())
                .build();

        //-- 결제 처리 요청 Command메시지 발송. 이 요청은 Axon서버가 Payment의 PaymentAggregate로 전달함
        try {
            commandGateway.sendAndWait(createPaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error("Error is occurred during handle <CreatePaymentCommand>: {}", e.getMessage());
            log.info("===== [Create Order] Compensate <CancelCreateOrderCommand>");
            //발송실패 또는 Command Handler 수행 에러 시 보상 처리인 주문 생성 취소 처리를 요청함
            compensatingService.cancelCreateOrder(aggregateIdMap);
        }
    }

    //-- 배송정보 생성 요청
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

    //-- 주문완료 처리 요청
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

    //-- 주문완료 후 처리
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedCreateOrderEvent event) {
        log.info("[Saga] [CompletedCreateOrderEvent] is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] Transaction is FINISHED =====");

        //-- Report service에 레포트 생성 요청
        compensatingService.updateReport(event.getOrderId(), true);
    }

    //================= 보상 처리 프로세스 =====================

    //-- 주문생성 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateOrderEvent event) {
        log.info("[Saga] <FailedCreateOrderEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    //-- 결제정보 생성 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreatePaymentEvent event) {
        log.info("[Saga] <FailedCreatePaymentEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    //-- 배송정보 생성 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCreateDeliveryEvent event) {
        log.info("[Saga] Handle <FailedCreateDeliveryEvent> for Order Id: {}", event.getOrderId());

        log.info("===== [Create Order] Compensate <CancelCreatePaymentCommand> ====");
        compensatingService.cancelCreatePayment(this.aggregateIdMap);
        log.info("===== [Create Order] Compensate <CancelCreateOrderCommand> ====");
        compensatingService.cancelCreateOrder(this.aggregateIdMap);
    }

    //-- 주문완료 처리 실패 시 보상 처리 요청
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

    //-- 주문 생성 취소 완료 시
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CancelledCreateOrderEvent event) {
        log.info("[Saga] CancelledCreateOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Create Order] Transaction is Aborted =====");
    }

}
