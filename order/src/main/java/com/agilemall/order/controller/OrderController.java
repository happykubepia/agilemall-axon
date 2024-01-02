package com.agilemall.order.controller;

import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.order.dto.OrderDTO;
import com.agilemall.order.dto.OrderDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private CommandGateway commandGateway;

    @PostMapping
    public String createOrder(@RequestBody OrderDTO orderDTO) {
        log.info("[@PostMapping] Executing createOrder: {}", orderDTO.toString());

        String orderId = "ORDER_"+RandomStringUtils.random(9, false, true);
        String paymentId = "PAY_"+RandomStringUtils.random(11, false, true);
        String userId = RandomStringUtils.randomAlphanumeric(10).toLowerCase() + "@gmail.com";

        //주문상세 주문ID, 주문금액 설정
        List<OrderDetailDTO> newOrderDetails = orderDTO.getOrderDetails().stream()
                .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getOrderSeq(), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        //*참고)Stream: https://futurecreator.github.io/2018/08/26/java-8-streams/

        //주문금액합계
        int totalOrderAmt = newOrderDetails.stream().mapToInt(OrderDetailDTO::getOrderAmt).sum();

        //결제정보 설정
        List<PaymentDetailDTO> newPaymentDetails = orderDTO.getPaymentDetails().stream()
                .map(p -> new PaymentDetailDTO(orderId, paymentId, p.getPaymentGbcd(), p.getPaymentAmt()))
                .collect(Collectors.toList());

        //결제금액 합계
        int totalPaymentAmt = newPaymentDetails.stream().mapToInt(PaymentDetailDTO::getPaymentAmt).sum();

        CreateOrderCommand createOrderCommand = CreateOrderCommand.builder()
                .orderId(orderId)
                .userId(userId)
                .totalOrderAmt(totalOrderAmt)
                .orderDetails(newOrderDetails)
                .paymentId(paymentId)
                .paymentDetails(newPaymentDetails)
                .totalPaymentAmt(totalPaymentAmt)
                .build();

        commandGateway.sendAndWait(createOrderCommand);

        return "Order Created";
    }
}
