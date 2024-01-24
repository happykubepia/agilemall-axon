package com.agilemall.order.saga;
/*
- 목적: 주문 수정 Saga 프로세스 처리
- 설명
    - 정상 처리: UpdatedOrderEvent -> UpdatedPaymentEvent -> CompletedUpdateOrderEvent
    - 실패 처리: 보상처리(Compensating tranaction) 수행
      - FailedUpdateOrderEvent: compensatingService.cancelUpdateOrder
      - FailedUpdatePaymentEvent: compensatingService.cancelUpdateOrder
      - FailedCompleteUpdateOrderEvent: compensatingService.cancelUpdatePayment -> compensatingService.cancelUpdateOrder
    - compensatingService.updateReportReport 호출하여 Report service의 조회용 데이터 갱신
*/
import com.agilemall.common.command.update.UpdatePaymentCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.update.FailedUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.order.command.CompleteUpdateOrderCommand;
import com.agilemall.order.events.*;
import com.agilemall.order.service.CompensatingService;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.modelling.saga.EndSaga;
import org.axonframework.modelling.saga.SagaEventHandler;
import org.axonframework.modelling.saga.SagaLifecycle;
import org.axonframework.modelling.saga.StartSaga;
import org.axonframework.spring.stereotype.Saga;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Saga
@Slf4j
public class OrderUpdatingSaga {
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

    //================== 정상 처리 ========================
    //-- 결제 정보 수정 처리 요청
    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(UpdatedOrderEvent event) {
        log.info("[Saga] UpdatedOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] #4: <UpdatePaymentCommand> =====");

        if(event.isCompensation()) {
            log.info("This event is compensation. So, Do nothing.");
            SagaLifecycle.end();
            return;
        }

        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), event.getOrderId());
        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), event.getPaymentId());

        UpdatePaymentCommand updatePaymentCommand = UpdatePaymentCommand.builder()
                .paymentId(event.getPaymentId())
                .orderId(event.getOrderId())
                .totalPaymentAmt(event.getTotalPaymentAmt())
                .paymentStatus(PaymentStatusEnum.CREATED.value())
                .paymentDetails(event.getPaymentDetails())
                .build();

        try {
            commandGateway.sendAndWait(updatePaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //throw new Exception("Error is occurred during handle <UpdatedOrderEvent>");
        } catch(Exception e) {
            log.info(e.getMessage());
            if(event.isCompensation()) {  //보상처리이면 Saga 종료(무한루프 방지)
                log.info("===== [Update Order] Transaction is Aborted =====");
                SagaLifecycle.end();
                return;
            }
            log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
            compensatingService.cancelUpdateOrder(aggregateIdMap);  //이전 주문 정보로 rollback 처리
        }
    }

    //-- 주문 수정 완료 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(UpdatedPaymentEvent event) {
        log.info("[Saga] UpdatedPaymentEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] #5: <CompletedOrderUpdateCommand> =====");

        if(event.isCompensation()) {
            log.info("This event is compensation. So, Do nothing.");
            return;
        }
        CompleteUpdateOrderCommand cmd = CompleteUpdateOrderCommand.builder()
                .orderId(event.getOrderId())
                .orderStatus(OrderStatusEnum.COMPLETED.value())
                .build();

        try {
            commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //throw new RuntimeException("주문수정 보상처리 테스트");
        } catch(Exception e) {
            log.error(e.getMessage());
            log.info("===== [Update Order] Compensation <CancelUpdatePaymentCommand> ==== ");
            compensatingService.cancelUpdatePayment(aggregateIdMap);      //이전 결제 정보로 rollback처리 요청
            log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
            compensatingService.cancelUpdateOrder(aggregateIdMap);  //이전 주문 정보로 rollback 처리
        }
    }

    //-- 주문 수정 최종 완료 시
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedUpdateOrderEvent event) {
        log.info("[Saga] [CompletedUpdateOrderEvent] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Updating Order] Transaction is Finished =====");

        //-- Report service에 주문, 결제, 배송 정보 업데이트: Report update처리를 CQRS패턴으로 변경하여 수행 안함
        //compensatingService.updateReport(event.getOrderId(), false);
    }

    //================= 보상 처리 =========================

    //-- 주문 정보 수정 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedUpdateOrderEvent event) {
        log.info("[Saga] FailedUpdateOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
        compensatingService.cancelUpdateOrder(aggregateIdMap);  //이전 주문 정보로 rollback 처리
    }

    //-- 결제 정보 수정 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedUpdatePaymentEvent event) {
        log.info("[Saga] FailedUpdatePaymentEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
        compensatingService.cancelUpdateOrder(aggregateIdMap);  //이전 주문 정보로 rollback 처리
    }

    //-- 주문 수정 완료 처리 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCompleteUpdateOrderEvent event) {
        log.info("[Saga] FailedCompleteUpdateOrderEvent is received for Order Id: {}", event.getOrderId());

        log.info("===== [Update Order] Compensation <CancelUpdatePaymentCommand> ==== ");
        compensatingService.cancelUpdatePayment(aggregateIdMap);      //이전 결제 정보로 rollback처리 요청
        log.info("===== [Update Order] Compensation <CancelUpdateOrderCommand> ==== ");
        compensatingService.cancelUpdateOrder(aggregateIdMap);        // 이전 주문 정보로 rollback처리 요청
    }

    //-- 주문 수정 취소 완료 시
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CancelledUpdateOrderEvent event) {
        log.info("[Saga] <CancelledUpdateOrderEvent> is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Update Order] Transaction is Aborted =====");

        //-- Report service에 주문, 결제, 배송 정보 업데이트
        compensatingService.updateReport(event.getOrderId(), false);
    }
}
