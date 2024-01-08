package com.agilemall.order.service;

import com.agilemall.common.command.CancelDeliveryCommand;
import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.command.CancelPaymentCommand;
import com.agilemall.common.dto.ServiceNameEnum;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Slf4j
@Service
public class CompensatingService {
    @Autowired
    private transient CommandGateway commandGateway;

    public void cancelOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelOrder> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = CancelOrderCommand.builder()
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value())).build();
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }

    }

    public void cancelPayment(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelPayment> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            //do compensating transaction: Payment
            CancelPaymentCommand cancelPaymentCommand = CancelPaymentCommand.builder()
                    .paymentId(aggregateIdMap.get(ServiceNameEnum.PAYMENT.value()))
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value()))
                    .build();
            commandGateway.sendAndWait(cancelPaymentCommand);

        } catch (Exception e) {
            log.error("Error is occurred during <cancelPaymentCommand>: {}", e.getMessage());
        }
    }

    public void cancelDelivery(HashMap<String, String> aggregateIdMap) {
        log.info("[CompensatingService] Executing <cancelDelivery> for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));
        try {
            //do compensating transaction: Delivery
            CancelDeliveryCommand cancelDeliveryCommand = CancelDeliveryCommand.builder()
                    .deliveryId(aggregateIdMap.get(ServiceNameEnum.DELIVERY.value()))
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value()))
                    .build();
            commandGateway.sendAndWait(cancelDeliveryCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelDeliveryCommand>: {}", e.getMessage());
        }
    }
}
