package com.agilemall.order.service;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.dto.OrderReqDetailDTO;

import java.util.List;

public interface InventoryService {
    List<ResultVO<InventoryDTO>> getInventory(List<OrderReqDetailDTO> orderDetails);
}
