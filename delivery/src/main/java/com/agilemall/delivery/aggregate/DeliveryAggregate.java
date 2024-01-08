package com.agilemall.delivery.aggregate;

import com.agilemall.common.command.CancelDeliveryCommand;
import com.agilemall.common.command.CreateDeliveryCommand;
import com.agilemall.common.events.DeliveryCancelledEvent;
import com.agilemall.common.events.DeliveryCreatedEvent;
import com.agilemall.delivery.command.UpdateDeliveryCommand;
import com.agilemall.delivery.events.DeliveryUpdatedEvent;
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
    private DeliveryAggregate(CreateDeliveryCommand createDeliveryCommand) {
        log.info("[@CommandHandler] Executing <CreateDeliveryCommand> for Order Id: {} and Delivery ID: {}",
                createDeliveryCommand.getOrderId(), createDeliveryCommand.getDeliveryId());

        DeliveryCreatedEvent deliveryCreatedEvent = new DeliveryCreatedEvent();
        BeanUtils.copyProperties(createDeliveryCommand, deliveryCreatedEvent);

        AggregateLifecycle.apply(deliveryCreatedEvent);
    }

    @EventSourcingHandler
    private void on(DeliveryCreatedEvent event) {
        log.info("[@EventSourcingHandler] Executing <DeliveryCreatedEvent> for Order Id: {} and Delivery Id: {}",
                event.getOrderId(), event.getDeliveryId());

        this.orderId = event.getOrderId();
        this.deliveryId = event.getDeliveryId();
        this.deliveryStatus = event.getDeliveryStatus();
    }

    @CommandHandler
    private void handle(CancelDeliveryCommand cancelDeliveryCommand) {
        log.info("[@CommandHandler] Executing <CancelDeliveryCommand> for Order Id : {} and Delivery Id: {}",
                cancelDeliveryCommand.getOrderId(), cancelDeliveryCommand.getDeliveryId());

        DeliveryCancelledEvent deliveryCancelledEvent = new DeliveryCancelledEvent();
        BeanUtils.copyProperties(cancelDeliveryCommand, deliveryCancelledEvent);

        AggregateLifecycle.apply(deliveryCancelledEvent);
    }

    @EventSourcingHandler
    private void on(DeliveryCancelledEvent event) {
        log.info("[@EventSourcingHandler] Executing <DeliveryCancelledEvent> for Order Id : {} and Delivery Id: {}",
                event.getOrderId(), event.getDeliveryId());
        this.deliveryStatus = event.getDeliveryStatus();
    }

    @CommandHandler
    private void handle(UpdateDeliveryCommand updateDeliveryCommand) {
        log.info("[@CommandHandler] Executing <DeliveryUpdateCommand> for Delivery Id : {}", updateDeliveryCommand.getDeliveryId());

        if(updateDeliveryCommand.getDeliveryStatus().equals(this.deliveryStatus)) {
            log.info("Delivery Status is already same value <{}>. So, This command is ignored", this.deliveryStatus);
            return;
        }
        DeliveryUpdatedEvent deliveryUpdatedEvent = new DeliveryUpdatedEvent();
        BeanUtils.copyProperties(updateDeliveryCommand, deliveryUpdatedEvent);

        AggregateLifecycle.apply(deliveryUpdatedEvent);
    }
    @EventSourcingHandler
    private void on(DeliveryUpdatedEvent event) {
        log.info("[@EventSourcing] Executing DeliveryUpdateEvent for Delivery Id : {}", event.getDeliveryId());
        this.deliveryStatus = event.getDeliveryStatus();
    }
}
