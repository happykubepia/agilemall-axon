package com.agilemall.common.events;

import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class InventoryQtyIncreaseEvent {
    private String inventoryId;
    private String orderId;
    private List<InventoryQtyAdjustDTO> inventoryQtyAdjustDetails;
}
