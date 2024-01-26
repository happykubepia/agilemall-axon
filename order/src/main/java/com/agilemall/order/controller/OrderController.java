package com.agilemall.order.controller;
/*
- 목적: 서비스 외부에 노출하는 API 정의
- 설명
  - createOder: 신규 주문 API
  - updateOrder: 주문 수정 API
  - deleteOrder: 주문 취소 API
*/

import com.agilemall.order.dto.OrderStatusDTO;
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

    private final OrderService orderService;
    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //-- 신규 주문 API
    @PostMapping("/orders")
    @Operation(summary = "신규 상품 주문 API")
    private ResultVO<CreateOrderCommand> createOrder(@RequestBody OrderReqCreateDTO orderReqCreateDTO) {
        log.info("[@PostMapping '/orders'] Executing createOrder: {}", orderReqCreateDTO.toString());

        ResultVO<CreateOrderCommand> retVo = orderService.createOrder(orderReqCreateDTO);

        log.info("[@PostMapping] Executing createOrder is Finished");
        return retVo;
    }

    //-- 주문 수정 API
    @PutMapping("/orders")
    @Operation(summary = "주문 수정 API")
    private ResultVO<UpdateOrderCommand> updateOrder(@RequestBody OrderReqUpdateDTO orderReqUpdateDTO) {
        log.info("[@PutMapping '/orders'] Executing updateOrder: {}", orderReqUpdateDTO.toString());

        return orderService.updateOrder(orderReqUpdateDTO);
    }

    //-- 주문 상세 정보 API
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "주문 상세현황 API")
    @Parameters({
            @Parameter(name = "orderId", in= ParameterIn.PATH, description = "주문ID", required = true)
    })
    private ResultVO<OrderStatusDTO> getOrderStaus(@PathVariable(name = "orderId") String orderId) {
        log.info("[@GetMapping '/orders/{userId}'] Executing getOrderStatus: {}", orderId);
        return orderService.getOrderStatus(orderId);
    }

    //-- 주문 취소 API
    @DeleteMapping("/orders/{orderId}")
    @Operation(summary = "주문 취소 API")
    @Parameters({
            @Parameter(name = "orderId", in= ParameterIn.PATH, description = "주문ID", required = true)
    })
    private ResultVO<String> deleteOrder(@PathVariable(name = "orderId") String orderId) {
        log.info("[@GetMapping '/orders/{userId}'] Executing deleteOrder: {}", orderId);
        return orderService.deleteOrder(orderId);
    }

}
