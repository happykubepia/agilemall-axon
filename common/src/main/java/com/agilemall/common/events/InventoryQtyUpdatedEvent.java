package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryQtyUpdatedEvent {
    private String productId;
    private String adjustType;
    private int adjustQty;
}
