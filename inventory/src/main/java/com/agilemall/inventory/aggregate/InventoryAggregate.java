package com.agilemall.inventory.aggregate;

import com.agilemall.common.command.CreateInventoryCommand;
import com.agilemall.common.command.InventoryQtyUpdateCommand;
import com.agilemall.common.dto.InventoryQtyAdjustType;
import com.agilemall.common.events.CreateInventoryEvent;
import com.agilemall.common.events.InventoryQtyUpdateEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

@Slf4j
@Aggregate
public class InventoryAggregate {
    @AggregateIdentifier
    private String productId;
    private String productName;

    private int unitPrice;
    private int inventoryQty;

    public InventoryAggregate() { }

    @CommandHandler
    public InventoryAggregate(CreateInventoryCommand createInventoryCommand) {
        log.info("[@CommandHandler] Executing CreateInventoryCommand for Product Id:{}", createInventoryCommand.getProductId());

        CreateInventoryEvent createInventoryEvent = new CreateInventoryEvent();
        BeanUtils.copyProperties(createInventoryCommand, createInventoryEvent);
        AggregateLifecycle.apply(createInventoryEvent);
    }

    @EventSourcingHandler
    public void on(CreateInventoryEvent event) {
        log.info("[@EventSourcingHandler] Executing CreateInventoryEvent");
        this.productId = event.getProductId();
        this.productName = event.getProductName();
        this.unitPrice = event.getUnitPrice();
        this.inventoryQty = event.getInventoryQty();
    }

    //보상 트랜잭션
    @CommandHandler
    public void handle(InventoryQtyUpdateCommand inventoryQtyUpdateCommand) {
        log.info("[@CommandHandler] Executing InventoryQtyUpdateCommand for productId:{}", inventoryQtyUpdateCommand.getProductId());

        InventoryQtyUpdateEvent inventoryQtyUpdateEvent = InventoryQtyUpdateEvent.builder()
                .productId(inventoryQtyUpdateCommand.getProductId())
                .adjustType(inventoryQtyUpdateCommand.getAdjustType())
                .adjustQty(inventoryQtyUpdateCommand.getAdjustQty())
                .build();

        AggregateLifecycle.apply(inventoryQtyUpdateEvent);
    }
    @EventSourcingHandler
    public void on(InventoryQtyUpdateEvent event) {
        log.info("[@EventSourcingHandler] Executing InventoryQtyUpdateEvent");
        this.productId = event.getProductId();
        if(InventoryQtyAdjustType.DECREASE.value().equals(event.getAdjustType())) {
            this.inventoryQty -= event.getAdjustQty();
        } else if(InventoryQtyAdjustType.INCREASE.value().equals(event.getAdjustType())) {
            this.inventoryQty += event.getAdjustQty();
        }
    }
}
