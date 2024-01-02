package com.agilemall.order.service;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import com.agilemall.order.dto.OrderDetailDTO;
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
        log.info("Executing isValidInventory");

        GetInventoryByProductIdQuery getInventoryByProductIdQuery;
        int reqQty;
        boolean existInventory = true;

        InventoryDTO inventoryDTO;
        try {
            for(OrderDetailDTO orderDetail:event.getOrderDetails()) {
                getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(orderDetail.getProductId());
                inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();
                reqQty = orderDetail.getQty();
                log.info("requestQty: {}, inventoryDTO.getInventoryQty: {}", reqQty, inventoryDTO.getInventoryQty());

                if (reqQty > inventoryDTO.getInventoryQty() || inventoryDTO.getInventoryQty() == 0) {
                    existInventory = false;
                    log.info("Product Id: {} => 재고 없음", orderDetail.getProductId());
                } else {
                    log.info("Product Id: {} => 재고 있음", orderDetail.getProductId());
                }

            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        return existInventory;
    }
}
