package com.agilemall.common.command;

import lombok.Builder;
import lombok.Value;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Value
@Builder
public class UpdateInventoryQtyCommand {
    @TargetAggregateIdentifier
    String productId;
    String adjustType;
    int adjustQty;
}
