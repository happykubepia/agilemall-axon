package com.agilemall.order.controller;

import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.order.command.UpdateOrderCommand;
import com.agilemall.order.dto.OrderReqCreateDTO;
import com.agilemall.order.dto.OrderReqUpdateDTO;
import com.agilemall.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Order service API", description="Order service API" )
@Slf4j
@RestController
@RequestMapping("/api/v1")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @PostMapping("/orders")
    @Operation(summary = "신규 상품 주문 API")
    private ResultVO<CreateOrderCommand> createOrder(@RequestBody OrderReqCreateDTO orderReqCreateDTO) {
        log.info("[@PostMapping '/orders'] Executing createOrder: {}", orderReqCreateDTO.toString());

        ResultVO<CreateOrderCommand> retVo = orderService.createOrder(orderReqCreateDTO);

        log.info("[@PostMapping] Executing createOrder is Finished");
        return retVo;
    }

    @PutMapping("/orders")
    @Operation(summary = "주문 수정 API")
    private ResultVO<UpdateOrderCommand> updateOrder(@RequestBody OrderReqUpdateDTO orderReqUpdateDTO) {
        log.info("[@PutMapping '/orders'] Executing updateOrder: {}", orderReqUpdateDTO.toString());
        ResultVO<UpdateOrderCommand> retVo = orderService.updateOrder(orderReqUpdateDTO);

        return retVo;
    }

    @DeleteMapping("/orders/{orderId}")
    @Operation(summary = "주문 취소 API")
    @Parameters({
            @Parameter(name = "orderId", in= ParameterIn.PATH, description = "주문ID", required = true, allowEmptyValue = false)
    })
    private ResultVO<String> deleteOrder(@PathVariable(name = "orderId", required = true) String orderId) {
        log.info("[@GetMapping '/orders/{userId}'] Executing deleteOrder: {}", orderId);
        ResultVO<String> retVo = orderService.deleteOrder(orderId);
        return retVo;
    }

}
