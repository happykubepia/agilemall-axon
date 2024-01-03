package com.agilemall.inventory.events;

import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import com.agilemall.common.dto.InventoryQtyAdjustType;
import com.agilemall.common.events.InventoryQtyAdjustedEvent;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class InventoryEventHandler {
    @Autowired
    InventoryRepository inventoryRepository;

    @EventHandler
    public void on(InventoryQtyAdjustedEvent event) {
        log.info("[@EventHandler] Executing on ..");

        Inventory inventory;
        for(InventoryQtyAdjustDTO reqAdjust:event.getInventoryQtyAdjustDetails()) {
            Optional <Inventory> optInventory = inventoryRepository.findById(reqAdjust.getProductId());
            if(optInventory.isPresent()) {
                inventory = optInventory.get();
                int qty = 0;
                if(InventoryQtyAdjustType.INCREASE.value().equals(reqAdjust.getAdjustType())) {
                    qty = inventory.getInventoryQty()+ reqAdjust.getAdjustQty();
                } else if(InventoryQtyAdjustType.DECREASE.value().equals(reqAdjust.getAdjustType())) {
                    qty = inventory.getInventoryQty() - reqAdjust.getAdjustQty();
                    if(qty < 0) qty = 0;
                }
                inventory.setInventoryQty(qty);
                inventoryRepository.save(inventory);
            }
        }
    }
}
