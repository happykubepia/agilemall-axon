package com.agilemall.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InventoryDTO {
    private String productId;
    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
