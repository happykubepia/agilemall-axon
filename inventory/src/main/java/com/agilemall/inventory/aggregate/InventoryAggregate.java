package com.agilemall.inventory.aggregate;

import com.agilemall.common.command.InventoryQtyAdjustCommand;
import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import com.agilemall.common.events.InventoryQtyAdjustedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.List;

@Slf4j
@Aggregate
public class InventoryAggregate {
    @AggregateIdentifier
    private String inventoryId;
    private String orderId;
    private List<InventoryQtyAdjustDTO> inventoryQtyAdjustDetails;

    public InventoryAggregate() { }

    @CommandHandler
    public InventoryAggregate(InventoryQtyAdjustCommand inventoryQtyAdjustCommand) {
        log.info("[@CommandHandler] Executing InventoryAggregate for InventoryId:{} and Order Id:{}", inventoryQtyAdjustCommand.getInventoryId(), inventoryQtyAdjustCommand.getOrderId());

        InventoryQtyAdjustedEvent inventoryQtyAdjustedEvent = InventoryQtyAdjustedEvent.builder()
                .inventoryId(inventoryQtyAdjustCommand.getInventoryId())
                .orderId(inventoryQtyAdjustCommand.getOrderId())
                .inventoryQtyAdjustDetails(inventoryQtyAdjustCommand.getInventoryQtyAdjustDetails())
                .build();

        AggregateLifecycle.apply(inventoryQtyAdjustedEvent);
    }

    @EventSourcingHandler
    public void on(InventoryQtyAdjustedEvent event) {
        log.info("[@EventSourcingHandler] Executing on ..");
        this.inventoryId = event.getInventoryId();
        this.orderId = event.getOrderId();
        this.inventoryQtyAdjustDetails = event.getInventoryQtyAdjustDetails();
    }
}
