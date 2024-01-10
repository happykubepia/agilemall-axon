package com.agilemall.common.command.create;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class CreateInventoryCommand {
    @TargetAggregateIdentifier
    String productId;

    String productName;
    int unitPrice;
    int inventoryQty;
}
