package com.agilemall.common.command;

import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@Builder
public class CreateInventoryCommand {
    @TargetAggregateIdentifier
    private String productId;

    private String productName;
    private int unitPrice;
    private int inventoryQty;
}
