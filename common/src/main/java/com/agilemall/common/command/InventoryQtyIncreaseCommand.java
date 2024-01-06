package com.agilemall.common.command;

import com.agilemall.common.dto.InventoryQtyAdjustDTO;
import lombok.Builder;
import lombok.Data;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.List;

@Data
@Builder
public class InventoryQtyIncreaseCommand {
    @TargetAggregateIdentifier
    private String inventoryId;
    private String orderId;
    private List<InventoryQtyAdjustDTO> inventoryQtyAdjustDetails;
}
