package com.agilemall.common.events.create;

import lombok.Data;

@Data
public class CreatedInventoryEvent {
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
