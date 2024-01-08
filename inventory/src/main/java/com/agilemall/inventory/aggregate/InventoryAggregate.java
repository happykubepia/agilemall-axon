package com.agilemall.inventory.aggregate;

import com.agilemall.common.command.CreateInventoryCommand;
import com.agilemall.common.command.UpdateInventoryQtyCommand;
import com.agilemall.common.dto.InventoryQtyAdjustTypeEnum;
import com.agilemall.common.events.InventoryCreatedEvent;
import com.agilemall.common.events.InventoryQtyUpdatedEvent;
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
        log.info("[@CommandHandler] Executing <CreateInventoryCommand> for Product Id:{}", createInventoryCommand.getProductId());

        InventoryCreatedEvent inventoryCreatedEvent = new InventoryCreatedEvent();
        BeanUtils.copyProperties(createInventoryCommand, inventoryCreatedEvent);
        AggregateLifecycle.apply(inventoryCreatedEvent);
    }

    @EventSourcingHandler
    public void on(InventoryCreatedEvent event) {
        log.info("[@EventSourcingHandler] Executing <CreateInventoryEvent>");
        this.productId = event.getProductId();
        this.productName = event.getProductName();
        this.unitPrice = event.getUnitPrice();
        this.inventoryQty = event.getInventoryQty();
    }

    //보상 트랜잭션
    @CommandHandler
    public void handle(UpdateInventoryQtyCommand updateInventoryQtyCommand) {
        log.info("[@CommandHandler] Executing <updateInventoryQtyCommand> for productId:{}", updateInventoryQtyCommand.getProductId());

        InventoryQtyUpdatedEvent inventoryQtyUpdatedEvent = InventoryQtyUpdatedEvent.builder()
                .productId(updateInventoryQtyCommand.getProductId())
                .adjustType(updateInventoryQtyCommand.getAdjustType())
                .adjustQty(updateInventoryQtyCommand.getAdjustQty())
                .build();

        AggregateLifecycle.apply(inventoryQtyUpdatedEvent);
    }
    @EventSourcingHandler
    public void on(InventoryQtyUpdatedEvent event) {
        log.info("[@EventSourcingHandler] Executing <InventoryQtyUpdatedEvent>");
        this.productId = event.getProductId();
        if(InventoryQtyAdjustTypeEnum.DECREASE.value().equals(event.getAdjustType())) {
            this.inventoryQty -= event.getAdjustQty();
        } else if(InventoryQtyAdjustTypeEnum.INCREASE.value().equals(event.getAdjustType())) {
            this.inventoryQty += event.getAdjustQty();
        }
    }
}
