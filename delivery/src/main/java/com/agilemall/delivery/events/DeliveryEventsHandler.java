package com.agilemall.delivery.events;

import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.command.CancelPaymentCommand;
import com.agilemall.common.command.UpdateReportDeliveryStatusCommand;
import com.agilemall.common.dto.DeliveryStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.DeliveryCreatedEvent;
import com.agilemall.delivery.entity.Delivery;
import com.agilemall.delivery.repository.DeliveryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
public class DeliveryEventsHandler {
    @Autowired
    private DeliveryRepository deliveryRepository;
    @Autowired
    private transient CommandGateway commandGateway;

    @EventHandler
    private void on(DeliveryCreatedEvent event) {
        log.info("[@EventHandler] Handle DeliveryCreatedEvent");
        try {
            Delivery delivery = new Delivery();
            BeanUtils.copyProperties(event, delivery);
            deliveryRepository.save(delivery);
        } catch(Exception e) {
            log.error("Error is occurred during handle DeliveryCreatedEvent: {}", e.getMessage());
            //-- request compensating transactions
            HashMap<String, String> aggregateIdMap = event.getAggregateIdMap();
            // compensate Payment
            cancelPayment(aggregateIdMap);
            // compensate Order
            cancelOrder(aggregateIdMap);
            //------------------------------

        }
    }

    @EventHandler
    private void on(DeliveryCancelledEvent event) {
        log.info("[@EventHandler] Handle DeliveryCancelledEvent");

        Delivery delivery = getEntry(event.getDeliveryId());
        if(delivery != null) {
            delivery.setDeliveryStatus(DeliveryStatusEnum.CANCELED.value());
            deliveryRepository.save(delivery);
        }
    }

    private void cancelPayment(HashMap<String, String> aggregateIdMap) {
        log.info("[DeliveryEventHandler] cancelPayment for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

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

    private void cancelOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[DeliveryEventHandler] cancelOrder for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = CancelOrderCommand.builder()
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value())).build();
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }
    }

    @EventHandler
    private void handle(DeliveryUpdatedEvent event) {
        log.info("[DeliveryEventsHandler] Handle <DeliveryUpdatedEvent> for Delivery Id: {}", event.getDeliveryId());

        Delivery delivery = getEntry(event.getDeliveryId());
        if(delivery != null) {
            delivery.setDeliveryStatus(event.getDeliveryStatus());
            deliveryRepository.save(delivery);

            //-- Send UpdateReportDeliveryStatusCommand to Report service
            UpdateReportDeliveryStatusCommand cmd = UpdateReportDeliveryStatusCommand.builder()
                    .orderId(event.getOrderId())
                    .deliveryStatus(event.getDeliveryStatus())
                    .build();

            commandGateway.send(cmd);
        }
    }

    private Delivery getEntry(String deliveryId) {
        Delivery delivery = null;
        Optional<Delivery> optDelivery = deliveryRepository.findById(deliveryId);
        if(optDelivery.isPresent()) {
            delivery = optDelivery.get();
        } else {
            log.info("Can't get entry for Delivery Id: {}", deliveryId);
        }
        return delivery;
    }
}

