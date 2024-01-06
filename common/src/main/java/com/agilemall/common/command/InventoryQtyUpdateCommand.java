package com.agilemall.common.command;

import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Data
@Builder
public class InventoryQtyUpdateCommand {
    @TargetAggregateIdentifier
    private String productId;
    private String adjustType;
    private int adjustQty;
}
