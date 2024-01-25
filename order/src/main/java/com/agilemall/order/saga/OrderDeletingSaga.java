package com.agilemall.order.saga;
/*
- 목적: 주문 취소를 위한 Saga Process 처리
- 설명
    - 정상 처리: DeletedOrderEvent -> DeletedPaymentEvent -> DeletedDeliveryEvent -> DeletedReportEvent -> CompletedDeleteOrderEvent
    - 실패 처리: 보상처리(Compensating tranaction) 수행
      - FailedDeletePaymentEvent: compensatingService.cancelDeleteOrder
      - FailedDeleteDeliveryEvent: compensatingService.cancelDeletePayment -> compensatingService.cancelDeleteOrder
      - FailedDeleteReportEvent: compensatingService.cancelDeleteDelivery -> compensatingService.cancelDeletePayment -> compensatingService.cancelDeleteOrder
      - FailedCompleteDeleteOrderEvent: compensatingService.cancelDeleteReport -> compensatingService.cancelDeleteDelivery -> compensatingService.cancelDeletePayment -> compensatingService.cancelDeleteOrder
*/
import com.agilemall.common.command.delete.DeleteDeliveryCommand;
import com.agilemall.common.command.delete.DeletePaymentCommand;
import com.agilemall.common.command.delete.DeleteReportCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.ReportDTO;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.delete.*;
import com.agilemall.order.command.CompleteDeleteOrderCommand;
import com.agilemall.order.events.CancelledDeleteOrderEvent;
import com.agilemall.order.events.CompletedDeleteOrderEvent;
import com.agilemall.order.events.DeletedOrderEvent;
import com.agilemall.order.events.FailedCompleteDeleteOrderEvent;
import com.agilemall.order.service.CompensatingService;
import lombok.extern.slf4j.Slf4j;
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
public class OrderDeletingSaga {
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

    private transient QueryGateway queryGateway;
    @Autowired
    public void setQueryGateway(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }
    //======================== 정상 처리 =============================

    //-- 결제 정보 삭제 요청
    @StartSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedOrderEvent event) {
        log.info("[Saga] DeletedOrderEvent is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] #2: <DeletePaymentCommand> =====");

        //--Report data에서 각 서비스의 Id값을 구함
        ReportDTO report = queryGateway.query(Constants.QUERY_REPORT, event.getOrderId(),
                ResponseTypes.instanceOf(ReportDTO.class)).join();
        if(report == null) {
            log.info("Can't find Report info for Order Id: {}", event.getOrderId());
            return;
        }
        aggregateIdMap.put(ServiceNameEnum.ORDER.value(), report.getOrderId());
        aggregateIdMap.put(ServiceNameEnum.PAYMENT.value(), report.getPaymentId());
        aggregateIdMap.put(ServiceNameEnum.DELIVERY.value(), report.getDeliveryId());
        aggregateIdMap.put(ServiceNameEnum.REPORT.value(), report.getReportId());

        //-- 결제 정보 삭제 요청 Command 객체 생성
        DeletePaymentCommand deletePaymentCommand = DeletePaymentCommand.builder()
                .paymentId(aggregateIdMap.get(ServiceNameEnum.PAYMENT.value()))
                .orderId(event.getOrderId())
                .build();

        //-- 결제 정보 삭제 요청 발송. Axon서버에 의해 Payment서비스의 PaymentAggregate로 요청됨
        try {
            commandGateway.sendAndWait(deletePaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //throw new Exception("Error is occurred during handle <DeletedOrderEvent>");
        } catch(Exception e) {
            log.info(e.getMessage());
            log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
            compensatingService.cancelDeleteOrder(aggregateIdMap);  //이전 주문 정보로 rollback 요청
        }
    }

    //-- 배송 정보 삭제 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedPaymentEvent event) {
        log.info("[Saga] <DeletedPaymentEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] #3: <DeleteDeliveryCommand> =====");

        //-- 배송 정보 삭제 요청 Command객체 생성
        DeleteDeliveryCommand deleteDeliveryCommand = DeleteDeliveryCommand.builder()
                .deliveryId(aggregateIdMap.get(ServiceNameEnum.DELIVERY.value()))
                .orderId(event.getOrderId())
                .build();

        //-- 배송 정보 삭제 요청 발송
        try {
            commandGateway.sendAndWait(deleteDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info(e.getMessage());
            log.info("===== [Delete Order] Compensation <CancelDeletePaymentCommand> =====");
            compensatingService.cancelDeletePayment(aggregateIdMap);    //이전 결제 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
            compensatingService.cancelDeleteOrder(aggregateIdMap);      //이전 주문 정보로 rollback 요청
        }
    }

    //-- 레포트 정보 삭제 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedDeliveryEvent event) {
        log.info("[Saga] <DeletedDeliveryEvent> is received for Order Id: "+ event.getOrderId());
        log.info("===== [Delete Order] #4: <DeleteReportCommand> =====");

        //-- 레포트 정보 삭제 요청 Command 객체 생성
        DeleteReportCommand deleteReportCommand = DeleteReportCommand.builder()
                .reportId(aggregateIdMap.get(ServiceNameEnum.REPORT.value()))
                .orderId(event.getOrderId())
                .build();

        //-- 레포트 정보 삭제 요청 발송
        try {
            commandGateway.sendAndWait(deleteReportCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error(e.getMessage());

            log.info("===== [Delete Order] Compensation <CancelDeleteDeliveryCommand> =====");
            compensatingService.cancelDeleteDelivery(aggregateIdMap);   //이전 배송 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeletePaymentCommand> =====");
            compensatingService.cancelDeletePayment(aggregateIdMap);    //이전 결제 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
            compensatingService.cancelDeleteOrder(aggregateIdMap);      //이전 주문 정보로 rollback 요청
        }
    }

    //-- 주문 삭제 최종 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedReportEvent event) {
        log.info("[Saga] <DeletedReportEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] #5: <CompleteDeleteOrderCommand> =====");

        //주문 삭제 최종 처리 요청 Command객체 생성
        CompleteDeleteOrderCommand completeDeleteOrderCommand = CompleteDeleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .build();

        //주문 삭제 최종 처리 요청
        try {
            //commandGateway.sendAndWait(completeDeleteOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            throw new RuntimeException("주문삭제 보상처리 테스트");
        } catch(Exception e) {
            log.error(e.getMessage());

            log.info("===== [Delete Order] Compensation <CancelDeleteDeliveryCommand> =====");
            compensatingService.cancelDeleteDelivery(aggregateIdMap);   //이전 배송 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeletePaymentCommand> =====");
            compensatingService.cancelDeletePayment(aggregateIdMap);    //이전 결제 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
            compensatingService.cancelDeleteOrder(aggregateIdMap);      //이전 주문 정보로 rollback 요청
            log.info("===== [Delete Order] Compensation <CancelDeleteReportCommand> =====");
            compensatingService.cancelDeleteReport(aggregateIdMap);     //이전 레포트 정보로 rollback 요청
        }
    }

    //-- 주문 삭제 최종 완료 시 처리
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedDeleteOrderEvent event) {
        log.info("[Saga] [CompletedDeleteOrderEvent] is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] Transaction is Finished =====");
    }

    //========================= 보상 처리 ============================

    //-- 결제 정보 삭제 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedDeletePaymentEvent event) {
        log.info("[Saga] <FailedDeletePaymentEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
        compensatingService.cancelDeleteOrder(aggregateIdMap);  //이전 주문 정보로 rollback 요청
    }

    //-- 배송 정보 삭제 실패 시 보상 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedDeleteDeliveryEvent event) {
        log.info("[Saga] <FailedDeleteDeliveryEvent> is received for Order Id: "+ event.getOrderId());
        log.info("===== [Delete Order] Compensation <CancelDeletePaymentCommand> =====");
        compensatingService.cancelDeletePayment(aggregateIdMap);    //이전 결제 정보로 rollback 요청
        log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
        compensatingService.cancelDeleteOrder(aggregateIdMap);      //이전 주문 정보로 rollback 요청
    }

    //-- 주문 삭제 최종 처리 실패 시 처리 요청
    @SagaEventHandler(associationProperty = "orderId")
    private void on(FailedCompleteDeleteOrderEvent event) {
        log.info("[Saga] [FailedCompleteDeleteOrderEvent] is received for Order Id: {}", event.getOrderId());

        log.info("===== [Delete Order] Compensation <CancelDeleteDeliveryCommand> =====");
        compensatingService.cancelDeleteDelivery(aggregateIdMap);   //이전 배송 정보로 rollback 요청
        log.info("===== [Delete Order] Compensation <CancelDeletePaymentCommand> =====");
        compensatingService.cancelDeletePayment(aggregateIdMap);    //이전 결제 정보로 rollback 요청
        log.info("===== [Delete Order] Compensation <CancelDeleteOrderCommand> =====");
        compensatingService.cancelDeleteOrder(aggregateIdMap);      //이전 주문 정보로 rollback 요청
        log.info("===== [Delete Order] Compensation <CancelDeleteReportCommand> =====");
        compensatingService.cancelDeleteReport(aggregateIdMap);     //이전 레포트 정보로 rollback 요청
    }

    //-- 주문 삭제 실패에 대한 보상 처리 완료 시
    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CancelledDeleteOrderEvent event) {
        log.info("[Saga] <CancelledDeleteOrderEvent> is received for Order Id: {}", event.getOrderId());

        log.info("===== [Delete Order] Transaction is Abortd =====");
    }
}
