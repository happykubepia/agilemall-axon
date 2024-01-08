package com.agilemall.common.events;

import lombok.Data;

@Data
public class InventoryCreatedEvent {
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
