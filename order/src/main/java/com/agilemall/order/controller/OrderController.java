package com.agilemall.order.controller;

import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.order.dto.OrderReqDTO;
import com.agilemall.order.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "Order service API", description="Order service API" )
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OrderController {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/orders")
    @Operation(summary = "신규 상품 주문 API")
    public String createOrder(@RequestBody OrderReqDTO orderReqDTO) {
        log.info("[@PostMapping] Executing createOrder: {}", orderReqDTO.toString());
        log.info("===== [OrderController] Transaction START =====");

        /*
        제품 재고 정보를 Query하여 재고 여부를 검사
        */
        log.info("===== [OrderController] Transaction #1: <isValidInventory> =====");
        List<ResultVO<InventoryDTO>> inventories = inventoryService.getInventory(orderReqDTO.getOrderReqDetails());
        String retCheck = isValidInventory(inventories);
        if (!retCheck.isEmpty()) {
            return retCheck;
        }

        String orderId = "ORDER_" + RandomStringUtils.random(9, false, true);
        String paymentId = "PAY_" + RandomStringUtils.random(11, false, true);
        //String userId = RandomStringUtils.randomAlphanumeric(10).toLowerCase() + "@gmail.com";
        String userId = orderReqDTO.getUserId();

        //주문상세 주문 ID, 주문금액 설정
        List<OrderDetailDTO> newOrderDetails = orderReqDTO.getOrderReqDetails().stream()
                .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getOrderSeq(), o.getQty(), o.getQty() * getUnitPrice(inventories, o.getProductId())))
                .collect(Collectors.toList());
        //*참고)Stream: https://futurecreator.github.io/2018/08/26/java-8-streams/

        //주문금액합계
        int totalOrderAmt = newOrderDetails.stream().mapToInt(OrderDetailDTO::getOrderAmt).sum();

        //결제정보 설정
        List<PaymentDetailDTO> newPaymentDetails = orderReqDTO.getPaymentReqDetails().stream()
                .map(p -> new PaymentDetailDTO(orderId, paymentId, p.getPaymentGbcd(), (int) Math.round(p.getPaymentRate() * totalOrderAmt)))
                .collect(Collectors.toList());

        //결제금액 합계
        int totalPaymentAmt = newPaymentDetails.stream().mapToInt(PaymentDetailDTO::getPaymentAmt).sum();

        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .orderId(orderId)
                .userId(userId)
                .orderDatetime(LocalDateTime.now())
                .totalOrderAmt(totalOrderAmt)
                .orderDetails(newOrderDetails)
                .paymentId(paymentId)
                .paymentDetails(newPaymentDetails)
                .totalPaymentAmt(totalPaymentAmt)
                .build();

        log.info("===== [OrderController] Transaction #2: <CreateOrderCommand> =====");
        log.info("[CreateOrderCommand] {}", createOrderCommand.toString());

        commandGateway.sendAndWait(createOrderCommand);

        return "Order Created";
    }

    private String isValidInventory (List < ResultVO < InventoryDTO >> inventories) {
        for (ResultVO<InventoryDTO> retVo : inventories) {
            if (!retVo.isReturnCode()) {
                return "재고없음: " + retVo.getResult().getProductId();
            }
        }
        return "";
    }

    private int getUnitPrice (List < ResultVO < InventoryDTO >> inventories, String productId){
        InventoryDTO inventory;
        for (ResultVO<InventoryDTO> retVo : inventories) {
            inventory = retVo.getResult();
            //log.info("==>{} vs {}", inventory.getProductId(), productId);
            if (inventory.getProductId().equals(productId)) {
                return inventory.getUnitPrice();
            }
        }
        return 0;
    }
}
