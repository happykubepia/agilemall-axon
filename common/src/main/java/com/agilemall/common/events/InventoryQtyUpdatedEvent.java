package com.agilemall.common.events;

import lombok.Data;

@Data
public class InventoryQtyUpdatedEvent {
    private String productId;
    private String adjustType;
    private int adjustQty;
}
