package com.agilemall.order.service;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.InventoryDTO;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.quries.GetInventoryByProductIdQuery;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.order.dto.OrderReqDTO;
import com.agilemall.order.dto.OrderReqDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;

    public ResultVO<CreateOrderCommand> createOrder(OrderReqDTO orderReqDTO) {
        log.info("[OrderService] Executing <createOrder>: {}", orderReqDTO.toString());
        log.info("===== [OrderController] Transaction START =====");

        ResultVO<CreateOrderCommand> retVo = new ResultVO<>();

        try {
            /*
            제품 재고 정보를 Query하여 재고 여부를 검사
            */
            log.info("===== [OrderService] Transaction #1: <isValidInventory> =====");
            List<ResultVO<InventoryDTO>> inventories = getInventory(orderReqDTO.getOrderReqDetails());
            String retCheck = isValidInventory(inventories);
            if (!retCheck.isEmpty()) {
                retVo.setReturnCode(false);
                retVo.setReturnMessage(retCheck);
                return retVo;
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

            log.info("===== [OrderService] Transaction #2: <CreateOrderCommand> =====");
            log.info("[CreateOrderCommand] {}", createOrderCommand.toString());

            commandGateway.sendAndWait(createOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Order Created");
            retVo.setResult(createOrderCommand);
        } catch(Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }


    private List<ResultVO<InventoryDTO>> getInventory(List<OrderReqDetailDTO> orderDetails) {
        log.info("[OrderService] Executing getInventory");

        GetInventoryByProductIdQuery getInventoryByProductIdQuery;
        List<ResultVO<InventoryDTO>> inventories = new ArrayList<>();
        ResultVO<InventoryDTO> retVo;
        int reqQty;

        InventoryDTO inventoryDTO;
        try {
            for(OrderReqDetailDTO orderDetail:orderDetails) {
                getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(orderDetail.getProductId());
                inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();
                reqQty = orderDetail.getQty();
                retVo = new ResultVO<>();
                retVo.setResult(inventoryDTO);
                retVo.setReturnCode(reqQty <= inventoryDTO.getInventoryQty() && inventoryDTO.getInventoryQty() != 0);
                inventories.add(retVo);
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        return inventories;
    }

    private String isValidInventory (List<ResultVO<InventoryDTO>> inventories) {
        log.info("[OrderService] Executing <isValidInventory>");

        for (ResultVO<InventoryDTO> retVo : inventories) {
            if (!retVo.isReturnCode()) {
                return "재고없음: " + retVo.getResult().getProductId();
            }
        }
        return "";
    }

    private int getUnitPrice (List<ResultVO<InventoryDTO>> inventories, String productId){
        log.info("[OrderService] Executing <getUnitPrice>");

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
