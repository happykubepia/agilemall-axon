package com.agilemall.inventory.query;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class InventoryProjection {
    @Autowired
    private InventoryRepository inventoryRepository;

    @QueryHandler
    public InventoryDTO getInventory(GetInventoryByProductIdQuery query) {
        log.info("[@QueryHandler] getInventoryByProductId in InventoryProjection for Product Id: {}", query.getProductId());

        int inventoryQty = 0;
        int unitPrice = 0;
        Optional <Inventory> optInventory = inventoryRepository.findById(query.getProductId());
        if(optInventory.isPresent()) {
            inventoryQty = optInventory.get().getInventoryQty();
            unitPrice = optInventory.get().getUnitPrice();
        }
        return new InventoryDTO(query.getProductId(), inventoryQty, unitPrice);
    }
}
