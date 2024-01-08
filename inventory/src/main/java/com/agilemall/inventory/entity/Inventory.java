package com.agilemall.inventory.entity;
/*
State Stored Aggregator
 */
import com.agilemall.common.command.CreateInventoryCommand;
import com.agilemall.common.command.UpdateInventoryQtyCommand;
import com.agilemall.common.events.InventoryCreatedEvent;
import com.agilemall.common.events.InventoryQtyUpdatedEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.io.Serial;
import java.io.Serializable;

@Slf4j
@Aggregate
@Data
@Entity
@Table(name = "inventory")
public class Inventory implements Serializable {
    @Serial
    private static final long serialVersionUID = 2169444340219001818L;

    @Id
    @AggregateIdentifier
    @Column(name = "product_id", nullable = false, length = 10)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "inventory_qty", nullable = false)
    private int inventoryQty;

    public Inventory() { }

    @CommandHandler
    private Inventory(CreateInventoryCommand createInventoryCommand) {
        log.info("[@CommandHandler] Executing <CreateInventoryCommand> for Product Id:{}", createInventoryCommand.getProductId());

        InventoryCreatedEvent inventoryCreatedEvent = new InventoryCreatedEvent();
        BeanUtils.copyProperties(createInventoryCommand, inventoryCreatedEvent);
        AggregateLifecycle.apply(inventoryCreatedEvent);
    }

    //--Aggregator 생성 Command에 대한 EventSourcingHandler는 반드시 있어야 함
    @EventSourcingHandler
    private void on(InventoryCreatedEvent event) {
        this.productId = event.getProductId();
        this.productName = event.getProductName();
        this.unitPrice = event.getUnitPrice();
        this.inventoryQty = event.getInventoryQty();
    }

    //보상 트랜잭션
    @CommandHandler
    private void handle(UpdateInventoryQtyCommand updateInventoryQtyCommand) {
        log.info("[@CommandHandler] Executing <updateInventoryQtyCommand> for productId:{}", updateInventoryQtyCommand.getProductId());

        InventoryQtyUpdatedEvent inventoryQtyUpdatedEvent = new InventoryQtyUpdatedEvent();
        BeanUtils.copyProperties(updateInventoryQtyCommand, inventoryQtyUpdatedEvent);

        AggregateLifecycle.apply(inventoryQtyUpdatedEvent);
    }
}
