package com.agilemall.delivery.aggregate;

import com.agilemall.common.command.CancelDeliveryCommand;
import com.agilemall.common.command.DeliveryOrderCommand;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.OrderDeliveredEvent;
import com.agilemall.common.dto.DeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Slf4j
@Aggregate
public class DeliveryAggregate {
    @AggregateIdentifier
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;

    public DeliveryAggregate() {

    }

    @CommandHandler
    public DeliveryAggregate(DeliveryOrderCommand deliveryOrderCommand) {
        log.info("[@CommandHandler] Executing DeliveryAggregate for Order Id: {} and Delivery ID: {}",
                deliveryOrderCommand.getOrderId(), deliveryOrderCommand.getDeliveryId());

        OrderDeliveredEvent orderDeliveredEvent = OrderDeliveredEvent.builder()
                .orderId(deliveryOrderCommand.getOrderId())
                .deliveryId(deliveryOrderCommand.getDeliveryId())
                .deliveryStatus(DeliveryStatus.CREATED.value())
                .aggregateIdMap(deliveryOrderCommand.getAggregateIdMap())
                .build();

        AggregateLifecycle.apply(orderDeliveredEvent);
    }

    @EventSourcingHandler
    public void on(OrderDeliveredEvent event) {
        log.info("[@EventSourcingHandler] Executing OrderDeliveriedEvent for Order Id: {} and Delivery Id: {}",
                event.getOrderId(), event.getDeliveryId());

        this.orderId = event.getOrderId();
        this.deliveryId = event.getDeliveryId();
        this.deliveryStatus = event.getDeliveryStatus();

    }

    @CommandHandler
    public void handle(CancelDeliveryCommand cancelDeliveryCommand) {
        log.info("Executing CancelDeliveryCommand for Order Id : {} and Delivery Id: {}",
                cancelDeliveryCommand.getOrderId(), cancelDeliveryCommand.getDeliveryId());

        DeliveryCancelledEvent deliveryCancelledEvent = new DeliveryCancelledEvent();
        BeanUtils.copyProperties(cancelDeliveryCommand, deliveryCancelledEvent);

        AggregateLifecycle.apply(deliveryCancelledEvent);
    }

    @EventSourcingHandler
    public void on(DeliveryCancelledEvent event) {
        log.info("Executing DeliveryCancelledEvent for Order Id : {} and Delivery Id: {}",
                event.getOrderId(), event.getDeliveryId());
        this.deliveryStatus = event.getDeliveryStatus();
    }
}
