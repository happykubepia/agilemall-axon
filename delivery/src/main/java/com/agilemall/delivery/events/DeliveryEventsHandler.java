package com.agilemall.delivery.events;

import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.command.CancelPaymentCommand;
import com.agilemall.common.dto.DeliveryStatus;
import com.agilemall.common.dto.ServiceName;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.OrderDeliveredEvent;
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
    public void on(OrderDeliveredEvent event) {
        log.info("[@EventHandler] Handle OrderDeliveredEvent");
        try {
            Delivery delivery = new Delivery();
            BeanUtils.copyProperties(event, delivery);
            deliveryRepository.save(delivery);
        } catch(Exception e) {
            log.error("Error is occurred during handle OrderDeliveredEvent: {}", e.getMessage());
            //-- request compensating transactions
            HashMap<String, String> aggregateIdMap = event.getAggregateIdMap();
            // compensate Payment
            compensatePayment(aggregateIdMap);
            // compensate Order
            compensateOrder(aggregateIdMap);
            //------------------------------

        }
    }

    @EventHandler
    public void on(DeliveryCancelledEvent event) {
        log.info("[@EventHandler] Handle DeliveryCancelledEvent");

        Delivery delivery = getEntry(event.getDeliveryId());
        if(delivery != null) {
            delivery.setDeliveryStatus(DeliveryStatus.CANCELED.value());
            deliveryRepository.save(delivery);
        }
    }

    private void compensatePayment(HashMap<String, String> aggregateIdMap) {
        log.info("[DeliveryEventHandler] cancelPayment for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));

        try {
            //do compensating transaction: Payment
            CancelPaymentCommand cancelPaymentCommand = new CancelPaymentCommand(
                    aggregateIdMap.get(ServiceName.PAYMENT.value()),
                    aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelPaymentCommand);

        } catch (Exception e) {
            log.error("Error is occurred during <cancelPaymentCommand>: {}", e.getMessage());
        }
    }

    private void compensateOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[DeliveryEventHandler] compensateOrder for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = new CancelOrderCommand(aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }
    }

    @EventHandler
    public void handle(DeliveryUpdateEvent event) {
        log.info("[DeliveryEventsHandler] Executing DeliveryUpdateEvent for Delivery Id: {}", event.getDeliveryId());

        Delivery delivery = getEntry(event.getDeliveryId());
        if(delivery != null) {
            delivery.setDeliveryStatus(event.getDeliveryStatus());
            deliveryRepository.save(delivery);
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

/*
    @EventHandler
    @Retryable(
            maxAttempts = Constants.RETRYABLE_MAXATTEMPTS,
            retryFor = { IOException.class, TimeoutException.class, RuntimeException.class},
            backoff = @Backoff(delay = Constants.RETRYABLE_DELAY)
    )
    public void on(ReportUpdateEvent event) {
        log.info("[@EventHandler] Handle ReportUpdateEvent");

    }
*/
}

