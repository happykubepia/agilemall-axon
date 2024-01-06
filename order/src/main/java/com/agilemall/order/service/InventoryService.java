package com.agilemall.order.service;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.dto.OrderReqDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class InventoryService {
    @Autowired
    private transient QueryGateway queryGateway;

    public List<ResultVO<InventoryDTO>> getInventory(List<OrderReqDetailDTO> orderDetails) {
        log.info("Executing getInventory");

        GetInventoryByProductIdQuery getInventoryByProductIdQuery;
        List<ResultVO<InventoryDTO>> inventories = new ArrayList<>();
        ResultVO<InventoryDTO> retVo;
        int reqQty;

        InventoryDTO inventoryDTO;
        try {
            for(OrderReqDetailDTO orderDetail:orderDetails) {
                getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(orderDetail.getProductId());
                inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();
                reqQty = orderDetail.getQty();
                retVo = new ResultVO<>();
                retVo.setResult(inventoryDTO);
                retVo.setReturnCode(reqQty <= inventoryDTO.getInventoryQty() && inventoryDTO.getInventoryQty() != 0);
                inventories.add(retVo);
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        return inventories;
    }
}
