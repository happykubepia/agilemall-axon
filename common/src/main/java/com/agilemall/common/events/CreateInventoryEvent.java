package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CreateInventoryEvent {
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
