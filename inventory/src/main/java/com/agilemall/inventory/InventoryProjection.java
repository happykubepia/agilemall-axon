package com.agilemall.inventory;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryProjection {
    @QueryHandler
    public InventoryDTO getInventoryByProductId(GetInventoryByProductIdQuery query) {
        log.info("[@QueryHandler] getInventoryByProductId in InventoryProjection for Product Id: {}", query.getProductId());

        //Test를 위해 재고 수량을 100개로 리턴
        InventoryDTO inventoryDTO = new InventoryDTO(query.getProductId(), 100);
        return inventoryDTO;
    }
}
