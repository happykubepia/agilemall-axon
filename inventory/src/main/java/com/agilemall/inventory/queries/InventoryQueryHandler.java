package com.agilemall.inventory.queries;
/*
- 목적: Query에 대한 요청을 처리하여 응답
- 설명:
    - Query를 요청할 때 Query명으로 하거나 Query객체로 할 수 있음
    - Query명으로 하는 경우는 Order, Delivery, Report의 Query Handler를 참조
    - Query객체로 요청은 아래와 같이 요청됨. Query객체에 Query수행에 필요한 정보가 같이 들어오게 됨.
    getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(orderDetail.getProductId());
    inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();
*/
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

    private final InventoryRepository inventoryRepository;
    @Autowired
    public InventoryQueryHandler(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @QueryHandler
    private InventoryDTO handle(GetInventoryByProductIdQuery query) {
        log.info("[@QueryHandler] Handle <GetInventoryByProductIdQuery> for Product Id: {}", query.getProductId());

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
