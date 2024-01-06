package com.agilemall.delivery.controller;

import com.agilemall.delivery.dto.DeliveryDTO;
import com.agilemall.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Delivery service API", description = "Delivery Application")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class DeliveryController {
    @Autowired
    DeliveryService deliveryService;

    @PutMapping("/delivery")
    @Operation(summary = "배송정보 업데이트", description = "deliveryStatus: 10(Created), 20(Cancelled), 30(Delivering), 40(Completed)")
    public String updateDeliveryStatus(@RequestBody DeliveryDTO deliveryDTO) {
        log.info("[@PutMapping] Executing updateDelivery: {}", deliveryDTO.toString());

        deliveryService.updateDeliveryStatus(deliveryDTO);
        return "Deliver Status Updated";
    }
}
