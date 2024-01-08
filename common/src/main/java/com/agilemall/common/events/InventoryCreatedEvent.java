package com.agilemall.common.events;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InventoryCreatedEvent {
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
