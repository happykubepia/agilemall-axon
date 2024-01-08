package com.agilemall.inventory.events;

import com.agilemall.common.dto.InventoryQtyAdjustTypeEnum;
import com.agilemall.common.events.InventoryCreatedEvent;
import com.agilemall.common.events.InventoryQtyUpdatedEvent;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
//@EnableRetry
public class InventoryEventHandler {
    @Autowired
    InventoryRepository inventoryRepository;

    @EventHandler
    private void on(InventoryCreatedEvent event) {
        log.info("[@EventHandler] Handle <InventoryCreatedEvent> for Product: {}", event.getProductName());

        Inventory inventory = new Inventory();
        inventory.setProductId(event.getProductId());
        inventory.setProductName(event.getProductName());
        inventory.setUnitPrice(event.getUnitPrice());
        inventory.setInventoryQty(event.getInventoryQty());

        inventoryRepository.save(inventory);
    }

    @EventHandler
    private void on(InventoryQtyUpdatedEvent event) {
        log.info("[@EventHandler] Handle <InventoryQtyUpdatedEvent> for Product Id: {}", event.getProductId());

        Optional <Inventory> optInventory = inventoryRepository.findById(event.getProductId());
        if(optInventory.isPresent()) {
            Inventory inventory = optInventory.get();
            int qty = 0;
            if(InventoryQtyAdjustTypeEnum.INCREASE.value().equals(event.getAdjustType())) {
                qty = inventory.getInventoryQty()+ event.getAdjustQty();
            } else if(InventoryQtyAdjustTypeEnum.DECREASE.value().equals(event.getAdjustType())) {
                qty = inventory.getInventoryQty() - event.getAdjustQty();
                if(qty < 0) qty = 0;
            }
            inventory.setInventoryQty(qty);
            inventoryRepository.save(inventory);
        }
    }

}
