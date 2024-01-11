package com.agilemall.order.saga;

import com.agilemall.common.command.delete.DeleteDeliveryCommand;
import com.agilemall.common.command.delete.DeletePaymentCommand;
import com.agilemall.common.command.delete.DeleteReportCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.ReportDTO;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.delete.DeletedDeliveryEvent;
import com.agilemall.common.events.delete.DeletedPaymentEvent;
import com.agilemall.common.events.delete.DeletedReportEvent;
import com.agilemall.order.command.CompleteDeleteOrderCommand;
import com.agilemall.order.events.CompletedDeleteOrderEvent;
import com.agilemall.order.events.DeletedOrderEvent;
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
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private CompensatingService compensatingService;

    private final HashMap<String, String> aggregateIdMap = new HashMap<>();

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

        DeletePaymentCommand deletePaymentCommand = DeletePaymentCommand.builder()
                .paymentId(aggregateIdMap.get(ServiceNameEnum.PAYMENT.value()))
                .orderId(event.getOrderId())
                .build();

        try {
            commandGateway.sendAndWait(deletePaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //throw new Exception("Error is occurred during handle <DeletedOrderEvent>");
        } catch(Exception e) {
            log.info(e.getMessage());
            //compensatingService.cancelUpdateOrder(aggregateIdMap);
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedPaymentEvent event) {
        log.info("[Saga] <DeletedPaymentEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] #3: <DeleteDeliveryCommand> =====");

        DeleteDeliveryCommand deleteDeliveryCommand = DeleteDeliveryCommand.builder()
                .deliveryId(aggregateIdMap.get(ServiceNameEnum.DELIVERY.value()))
                .orderId(event.getOrderId())
                .build();

        try {
            commandGateway.sendAndWait(deleteDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.info(e.getMessage());
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedDeliveryEvent event) {
        log.info("[Saga] <DeletedDeliveryEvent> is received for Order Id: "+ event.getOrderId());
        log.info("===== [Delete Order] #4: <DeleteReportCommand> =====");

        DeleteReportCommand deleteReportCommand = DeleteReportCommand.builder()
                .reportId(aggregateIdMap.get(ServiceNameEnum.REPORT.value()))
                .orderId(event.getOrderId())
                .build();

        try {
            commandGateway.sendAndWait(deleteReportCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }

    @SagaEventHandler(associationProperty = "orderId")
    private void on(DeletedReportEvent event) {
        log.info("[Saga] <DeletedReportEvent> is received for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] #5: <CompleteDeleteOrderCommand> =====");

        CompleteDeleteOrderCommand completeDeleteOrderCommand = CompleteDeleteOrderCommand.builder()
                .orderId(event.getOrderId())
                .build();

        try {
            commandGateway.sendAndWait(completeDeleteOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }


    @EndSaga
    @SagaEventHandler(associationProperty = "orderId")
    private void on(CompletedDeleteOrderEvent event) {
        log.info("[Saga] [CompletedDeleteOrderEvent] is finished for Order Id: {}", event.getOrderId());
        log.info("===== [Delete Order] Transaction is Finished =====");

    }

}
