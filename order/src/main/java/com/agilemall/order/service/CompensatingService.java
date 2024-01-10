package com.agilemall.order.service;

import com.agilemall.common.command.create.CancelCreateDeliveryCommand;
import com.agilemall.common.command.create.CancelCreateOrderCommand;
import com.agilemall.common.command.create.CancelCreatePaymentCommand;
import com.agilemall.common.command.create.CreateReportCommand;
import com.agilemall.common.command.update.CancelUpdatePaymentCommand;
import com.agilemall.common.command.update.UpdateReportCommand;
import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.DeliveryDTO;
import com.agilemall.common.dto.OrderDTO;
import com.agilemall.common.dto.PaymentDTO;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.queries.GetReportId;
import com.agilemall.order.command.CancelUpdateOrderCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CompensatingService {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;

    public void cancelCreateOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelCreateOrder> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            CancelCreateOrderCommand cancelCreateOrderCommand = CancelCreateOrderCommand.builder()
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value())).build();
            commandGateway.sendAndWait(cancelCreateOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelCreateOrder>: {}", e.getMessage());
        }
    }

    public void cancelCreatePayment(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelCreatePayment> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            //do compensating transaction: Payment
            CancelCreatePaymentCommand cancelCreatePaymentCommand = CancelCreatePaymentCommand.builder()
                    .paymentId(aggregateIdMap.get(ServiceNameEnum.PAYMENT.value()))
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value()))
                    .build();
            commandGateway.sendAndWait(cancelCreatePaymentCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Error is occurred during <cancelCreatePayment>: {}", e.getMessage());
        }
    }

    public void cancelCreateDelivery(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelCreateDelivery> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));
        try {
            //compensating transaction: Delivery
            CancelCreateDeliveryCommand cancelCreateDeliveryCommand = CancelCreateDeliveryCommand.builder()
                    .deliveryId(aggregateIdMap.get(ServiceNameEnum.DELIVERY.value()))
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value()))
                    .build();
            commandGateway.sendAndWait(cancelCreateDeliveryCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelCreateDelivery>: {}", e.getMessage());
        }
    }

    public void cancelUpdateOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelUpdateOrder> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));
        try {
            commandGateway.sendAndWait(CancelUpdateOrderCommand.builder()
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value())).isCompensation(true)
                    .build(), Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);

        } catch(Exception e) {
            log.error("Error is occurred during <cancelUpdateOrder>: {}", e.getMessage());
        }
    }

    public void cancelUpdatePayment(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelUpdatePayment> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));
        try {
            commandGateway.sendAndWait(CancelUpdatePaymentCommand.builder()
                    .paymentId(aggregateIdMap.get(ServiceNameEnum.PAYMENT.value()))
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value()))
                    .isCompensation(true)
                    .build(), Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);

        } catch(Exception e) {
            log.error("Error is occurred during <cancelUpdatePayment>: {}", e.getMessage());
        }
    }

    public void updateReport(String orderId, boolean isCreate) {
        log.info("===== START Updating Report =====");

        try {
            OrderDTO order = queryGateway.query(Constants.QUERY_REPORT, orderId,
                    ResponseTypes.instanceOf(OrderDTO.class)).join();
            PaymentDTO payment = queryGateway.query(Constants.QUERY_REPORT, orderId,
                    ResponseTypes.instanceOf(PaymentDTO.class)).join();
            DeliveryDTO delivery = queryGateway.query(Constants.QUERY_REPORT, orderId,
                    ResponseTypes.instanceOf(DeliveryDTO.class)).join();

            if(isCreate) {
                CreateReportCommand cmd = CreateReportCommand.builder()
                        .reportId(RandomStringUtils.random(15, false, true))
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
                commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            } else {
                String reportId = queryGateway.query(new GetReportId(orderId),
                        ResponseTypes.instanceOf(String.class)).join();
                if("".equals(reportId)) {
                    log.info("Can't get Report Id for Order Id: {}", orderId);
                    return;
                }
                UpdateReportCommand cmd = UpdateReportCommand.builder()
                        .reportId(reportId)
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
                commandGateway.sendAndWait(cmd, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            }

            log.info("===== END Updating Report =====");
        }  catch(Exception e) {
            log.info(e.getMessage());
        }
    }
}
