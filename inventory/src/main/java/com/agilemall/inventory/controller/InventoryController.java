package com.agilemall.inventory.controller;

import com.agilemall.common.vo.ResultVO;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory service API", description = "Inventory Application")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class InventoryController {

    @Autowired
    private final InventoryService inventoryService;

    @PostMapping("/register")
    @Operation(summary = "신규 제품 등록")
    public ResponseEntity<ResultVO<Inventory>> register(@RequestBody Inventory inventory) {
        return inventoryService.register(inventory);
    }
}
