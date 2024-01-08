package com.agilemall.order.controller;

import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.order.dto.OrderReqDTO;
import com.agilemall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Order service API", description="Order service API" )
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    @Operation(summary = "신규 상품 주문 API")
    public ResultVO<CreateOrderCommand> createOrder(@RequestBody OrderReqDTO orderReqDTO) {
        log.info("[@PostMapping '/orders'] Executing createOrder: {}", orderReqDTO.toString());

        ResultVO<CreateOrderCommand> retVo = orderService.createOrder(orderReqDTO);

        log.info("[@PostMapping] Executing createOrder is Finished");
        return retVo;
    }
}
