package com.agilemall.inventory.queries;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.queries.GetInventoryByProductIdQuery;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.repository.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class InventoryQueryHandler {
    @Autowired
    private InventoryRepository inventoryRepository;

    @QueryHandler
    private InventoryDTO handle(GetInventoryByProductIdQuery query) {
        log.info("[@QueryHandler] Handle <GetInventoryByProductIdQuery> for Product Id: {}", query.getProductId());

        int inventoryQty = 0;
        int unitPrice = 0;
        Optional <Inventory> optInventory = inventoryRepository.findById(query.getProductId());
        if(optInventory.isPresent()) {
            Inventory inventory = optInventory.get();
            return new InventoryDTO(
                    inventory.getProductId(), inventory.getProductName(),
                    inventory.getUnitPrice(), inventory.getInventoryQty());
        } else {
            return null;
        }
    }
}
