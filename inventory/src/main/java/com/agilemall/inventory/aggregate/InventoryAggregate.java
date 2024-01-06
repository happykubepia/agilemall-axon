package com.agilemall.inventory.aggregate;

import com.agilemall.common.command.InventoryQtyDecreaseCommand;
import com.agilemall.common.command.InventoryQtyIncreaseCommand;
import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import com.agilemall.common.events.InventoryQtyDecreaseEvent;
import com.agilemall.common.events.InventoryQtyIncreaseEvent;
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
    public InventoryAggregate(InventoryQtyDecreaseCommand inventoryQtyDecreaseCommand) {
        log.info("[@CommandHandler] Executing InventoryQtyDecreaseCommand for InventoryId:{} and Order Id:{}", inventoryQtyDecreaseCommand.getInventoryId(), inventoryQtyDecreaseCommand.getOrderId());

        InventoryQtyDecreaseEvent inventoryQtyDecreaseEvent = InventoryQtyDecreaseEvent.builder()
                .inventoryId(inventoryQtyDecreaseCommand.getInventoryId())
                .orderId(inventoryQtyDecreaseCommand.getOrderId())
                .inventoryQtyAdjustDetails(inventoryQtyDecreaseCommand.getInventoryQtyAdjustDetails())
                .aggregateIdMap(inventoryQtyDecreaseCommand.getAggregateIdMap())
                .build();

        AggregateLifecycle.apply(inventoryQtyDecreaseEvent);
    }

    @EventSourcingHandler
    public void on(InventoryQtyDecreaseEvent event) {
        log.info("[@EventSourcingHandler] Executing InventoryQtyDecreaseEvent");
        this.inventoryId = event.getInventoryId();
        this.orderId = event.getOrderId();
        this.inventoryQtyAdjustDetails = event.getInventoryQtyAdjustDetails();
    }

    //보상 트랜잭션
    @CommandHandler
    public void handle(InventoryQtyIncreaseCommand inventoryQtyIncreaseCommand) {
        log.info("[@CommandHandler] Executing InventoryQtyIncreaseCommand for InventoryId:{} and Order Id:{}",
                inventoryQtyIncreaseCommand.getInventoryId(), inventoryQtyIncreaseCommand.getOrderId());

        InventoryQtyIncreaseEvent inventoryQtyIncreaseEvent = InventoryQtyIncreaseEvent.builder()
                .inventoryId(inventoryQtyIncreaseCommand.getInventoryId())
                .orderId(inventoryQtyIncreaseCommand.getOrderId())
                .inventoryQtyAdjustDetails(inventoryQtyIncreaseCommand.getInventoryQtyAdjustDetails())
                .build();

        AggregateLifecycle.apply(inventoryQtyIncreaseEvent);
    }
    @EventSourcingHandler
    public void on(InventoryQtyIncreaseEvent event) {
        log.info("[@EventSourcingHandler] Executing InventoryQtyIncreaseEvent");
        this.inventoryId = event.getInventoryId();
        this.orderId = event.getOrderId();
        this.inventoryQtyAdjustDetails = event.getInventoryQtyAdjustDetails();
    }
}
