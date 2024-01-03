package com.agilemall.inventory.service;

import com.agilemall.common.vo.ResultVO;
import com.agilemall.inventory.entity.Inventory;
import com.agilemall.inventory.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;

    public ResponseEntity<ResultVO<Inventory>> register(Inventory inventory) {
        ResultVO<Inventory> result = new ResultVO<>();

        try {
            Inventory newInventory = inventoryRepository.save(inventory);
            result.setReturnCode(true);
            result.setReturnMessage("New inventory is Created");
            result.setResult(newInventory);
        } catch(Exception e) {
            result.setReturnCode(false);
            result.setReturnMessage(e.getMessage());
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
