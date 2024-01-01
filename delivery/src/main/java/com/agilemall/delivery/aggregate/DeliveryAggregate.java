package com.agilemall.delivery.aggregate;

import com.agilemall.common.command.DeliveryOrderCommand;
import com.agilemall.common.events.OrderDeliveriedEvent;
import com.agilemall.delivery.dto.DeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

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

        OrderDeliveriedEvent orderDeliveriedEvent = OrderDeliveriedEvent.builder()
                .orderId(deliveryOrderCommand.getOrderId())
                .deliveryId(deliveryOrderCommand.getDeliveryId())
                .deliveryStatus(DeliveryStatus.REQUESTED.value())
                .build();

        AggregateLifecycle.apply(orderDeliveriedEvent);
    }

    @EventSourcingHandler
    public void on(OrderDeliveriedEvent event) {
        log.info("[@EventSourcingHandler] Executing OrderDeliveriedEvent for Order Id: {} and Delivery Id: {}",
                event.getOrderId(), event.getDeliveryId());

        this.orderId = event.getOrderId();
        this.deliveryId = event.getDeliveryId();
        this.deliveryStatus = event.getDeliveryStatus();

    }
}
