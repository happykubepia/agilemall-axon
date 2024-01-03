package com.agilemall.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryQtyAdjustDTO {
    private String productId;
    private String adjustType;
    private int adjustQty;
}
