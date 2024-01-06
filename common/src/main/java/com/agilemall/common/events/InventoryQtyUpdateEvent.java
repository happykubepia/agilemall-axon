package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryQtyUpdateEvent {
    private String productId;
    private String adjustType;
    private int adjustQty;
}
