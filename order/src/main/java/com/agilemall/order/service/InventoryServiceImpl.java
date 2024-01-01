package com.agilemall.order.service;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import com.agilemall.order.events.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {
    @Autowired
    private transient QueryGateway queryGateway;

    @Override
    public boolean isValidInventory(OrderCreatedEvent event) {
        int totalRequestQty = event.getOrderDetails().stream().mapToInt(o -> o.getQty()).sum();
        boolean isValidInventory = false;

        GetInventoryByProductIdQuery getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(event.getOrderId());

        InventoryDTO inventoryDTO;
        try {
            inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();
            log.info("totalRequestQty: {}, inventoryDTO.getInventoryQty: {}", totalRequestQty, inventoryDTO.getInventoryQty());

            if(totalRequestQty <= inventoryDTO.getInventoryQty() || inventoryDTO.getInventoryQty() != 0) {
                isValidInventory = true;
            }
            log.info("재고여부: {}", isValidInventory);
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        return isValidInventory;
    }
}
