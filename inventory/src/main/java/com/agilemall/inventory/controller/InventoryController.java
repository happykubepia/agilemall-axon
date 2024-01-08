package com.agilemall.inventory.controller;

import com.agilemall.common.command.CreateInventoryCommand;
import com.agilemall.inventory.entity.Inventory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory service API", description = "Inventory Application")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InventoryController {
    @Autowired
    private transient CommandGateway commandGateway;

    @PostMapping("/register")
    @Operation(summary = "신규 제품 등록")
    public void register(@RequestBody Inventory inventory) {
        log.info("[@PostMapping(\"/register\")] Executing register: {}", inventory.toString());

        CreateInventoryCommand createInventoryCommand = CreateInventoryCommand.builder()
                .productId(inventory.getProductId())
                .productName(inventory.getProductName())
                .unitPrice(inventory.getUnitPrice())
                .inventoryQty(inventory.getInventoryQty())
                .build();
        commandGateway.sendAndWait(createInventoryCommand);
    }
}
