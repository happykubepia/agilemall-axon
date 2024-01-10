package com.agilemall.common.events.update;

import lombok.Data;

@Data
public class UpdatedInventoryQtyEvent {
    private String productId;
    private String adjustType;
    private int adjustQty;
}
