package com.agilemall.order.service;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.*;
import com.agilemall.common.queries.GetInventoryByProductIdQuery;
import com.agilemall.common.vo.ResultVO;
import com.agilemall.order.command.CreateOrderCommand;
import com.agilemall.order.command.UpdateOrderCommand;
import com.agilemall.order.dto.OrderReqCreateDTO;
import com.agilemall.order.dto.OrderReqDetailDTO;
import com.agilemall.order.dto.OrderReqUpdateDTO;
import com.agilemall.order.dto.PaymentReqDetailDTO;
import com.agilemall.order.entity.Order;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.repository.OrderRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private transient CommandGateway commandGateway;
    @Autowired
    private transient QueryGateway queryGateway;
    @Autowired
    private OrderRepository orderRepository;

    public ResultVO<CreateOrderCommand> createOrder(OrderReqCreateDTO orderReqCreateDTO) {
        log.info("[OrderService] Executing <createOrder>: {}", orderReqCreateDTO.toString());
        log.info("===== [OrderService] Creating Order Transaction START =====");

        ResultVO<CreateOrderCommand> retVo = new ResultVO<>();
        //-- 결제수단과 비율 입력값 유효성 체크
        List<PaymentReqDetailDTO> payDetails = orderReqCreateDTO.getPaymentReqDetails();
        ResultVO<String> retPay = isValidPaymentInput(payDetails);
        if(!retPay.isReturnCode()) {
            log.info(retPay.getReturnMessage());
            retVo.setReturnCode(retPay.isReturnCode());
            retVo.setReturnMessage(retPay.getReturnMessage());
            return retVo;
        }

        try {
            /*
            제품 재고 정보를 Query하여 재고 여부를 검사
            */
            log.info("===== [OrderService] Creating Order Transaction #1: <isValidInventory> =====");
            List<ResultVO<InventoryDTO>> inventories = getInventory(orderReqCreateDTO.getOrderReqDetails());
            String retCheck = isValidInventory(inventories);
            if (!retCheck.isEmpty()) {
                retVo.setReturnCode(false);
                retVo.setReturnMessage(retCheck);
                return retVo;
            }

            String orderId = "ORDER_" + RandomStringUtils.random(9, false, true);
            String paymentId = "PAY_" + RandomStringUtils.random(11, false, true);
            //String userId = RandomStringUtils.randomAlphanumeric(10).toLowerCase() + "@gmail.com";
            String userId = orderReqCreateDTO.getUserId();

            //주문상세 주문 ID, 주문금액 설정
            List<OrderDetailDTO> newOrderDetails = orderReqCreateDTO.getOrderReqDetails().stream()
                    .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getQty(), o.getQty() * getUnitPrice(inventories, o.getProductId())))
                    .collect(Collectors.toList());
            //*참고)Stream: https://futurecreator.github.io/2018/08/26/java-8-streams/

            //주문금액합계
            int totalOrderAmt = newOrderDetails.stream().mapToInt(OrderDetailDTO::getOrderAmt).sum();

            //결제정보 설정
            List<PaymentDetailDTO> newPaymentDetails = orderReqCreateDTO.getPaymentReqDetails().stream()
                    .map(p -> new PaymentDetailDTO(orderId, paymentId, p.getPaymentKind(), (int) Math.round(p.getPaymentRate() * totalOrderAmt)))
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

            log.info("===== [OrderService] Creating Order Transaction #2: <CreateOrderCommand> =====");
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

    public ResultVO<UpdateOrderCommand> updateOrder(OrderReqUpdateDTO orderReqUpdateDTO) {
        log.info("[OrderService] Executing <updateOrder>: {}", orderReqUpdateDTO.toString());
        log.info("===== [OrderService] Updating Order Transaction START =====");

        String orderId = orderReqUpdateDTO.getOrderId();
        ResultVO<String> retCheck;
        ResultVO<UpdateOrderCommand> retVo = new ResultVO<>();

        //-- 결제수단과 비율 입력값 유효성 체크
        List<PaymentReqDetailDTO> payDetails = orderReqUpdateDTO.getPaymentReqDetails();
        retCheck = isValidPaymentInput(payDetails);
        if(!retCheck.isReturnCode()) {
            log.info(retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        retVo = new ResultVO<>();
        /*
        Validate Request
        */
        //-- 현재 배송상태로 읽어 주문 수정 가능한지 검사
        log.info("===== [OrderService] Updating Order Transaction #1: <validateOrderUpdatableByDeliveryStatus> =====");
        retCheck = validateOrderUpdatableByDeliveryStatus(orderId);
        if(!retCheck.isReturnCode()) {
            log.info("NOT Updatable: {}", retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        //-- 현재 주문 정보를 읽어 주문 수정 가능한지 검사
        log.info("===== [OrderService] Updating Order Transaction #2: <validateOrderUpdatableByProduct> =====");
        retCheck = validateOrderUpdatableByProduct(orderReqUpdateDTO);
        if(!retCheck.isReturnCode()) {
            log.info("NOT Updatable: {}", retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        //-- 주문 제품의 재고상태를 읽어 주문 수정 가능한지 검사
        log.info("===== [OrderService] Updating Order Transaction #3: <isValidInventory> =====");
        List<ResultVO<InventoryDTO>> inventories = getInventory(orderReqUpdateDTO.getOrderReqDetails());
        String strCheck = isValidInventory(inventories);
        if (!strCheck.isEmpty()) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(strCheck);
            return retVo;
        }

        /*
        send UpdateOrderCommand
        */
        log.info("===== [OrderService] Updating Order Transaction #4: <updateOrderCommand> =====");

        List<OrderDetailDTO> newOrderDetails = orderReqUpdateDTO.getOrderReqDetails().stream()
                .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getQty(), o.getQty() * getUnitPrice(inventories, o.getProductId())))
                .collect(Collectors.toList());

        //주문금액합계
        int totalOrderAmt = newOrderDetails.stream().mapToInt(OrderDetailDTO::getOrderAmt).sum();

        //결제정보 설정
        PaymentDTO payment = queryGateway.query(Constants.QUERY_REPORT, orderId,
                ResponseTypes.instanceOf(PaymentDTO.class)).join();
        if(payment == null) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("주문ID <"+orderId+">에 대한 결제정보를 찾을 수 없음");
            return retVo;
        }

        List<PaymentDetailDTO> newPaymentDetails = orderReqUpdateDTO.getPaymentReqDetails().stream()
                .map(p -> new PaymentDetailDTO(orderId, payment.getPaymentId(), p.getPaymentKind(), (int) Math.round(p.getPaymentRate() * totalOrderAmt)))
                .collect(Collectors.toList());

        //결제금액 합계
        int totalPaymentAmt = newPaymentDetails.stream().mapToInt(PaymentDetailDTO::getPaymentAmt).sum();

        UpdateOrderCommand updateOrderCommand = UpdateOrderCommand.builder()
                .orderId(orderReqUpdateDTO.getOrderId())
                .orderDatetime(LocalDateTime.now())
                .totalOrderAmt(totalOrderAmt)
                .orderDetails(newOrderDetails)
                .paymentId(payment.getPaymentId())
                .paymentDetails(newPaymentDetails)
                .totalPaymentAmt(totalPaymentAmt)
                .build();

        try {
            commandGateway.sendAndWait(updateOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Succes to request <updateOrderCommand>");
            retVo.setResult(updateOrderCommand);

        } catch(Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }

    /*
    =============== Private Method =======================
    */
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

    private ResultVO<String> isValidPaymentInput(List<PaymentReqDetailDTO> paymentDetails) {
        ResultVO<String> retVo = new ResultVO<>();
        //지불수단 유효성 체크
        for(PaymentReqDetailDTO item:paymentDetails) {
            Optional<PaymentKindEnum> optEnum = Arrays.stream(PaymentKindEnum.values())
                    .filter(o -> o.value().equals(item.getPaymentKind()))
                    .findFirst();
            if(optEnum.isEmpty()) {
                retVo.setReturnCode(false);
                retVo.setReturnMessage("지불수단 <"+item.getPaymentKind()+">은 정상적인 지불 수단 아님");
                return retVo;
            }
        }

        //결제 비율의 합이 1인지 체크
        if(paymentDetails.stream().mapToDouble(PaymentReqDetailDTO::getPaymentRate).sum() != 1) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("결제 비율의 합은 1이어야 함");
            return retVo;
        }

        retVo.setReturnCode(true);
        retVo.setReturnMessage("유효한 결제수단 및 결제비율임");
        return retVo;
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

        Optional<ResultVO<InventoryDTO>> optInventory = inventories.stream()
                .filter( obj -> productId.equals(obj.getResult().getProductId()))
                .findFirst();
        return (optInventory.map(inventoryDTOResultVO -> inventoryDTOResultVO.getResult().getUnitPrice()).orElse(0));
    }

    /*
    - 기능: 배송상태가 취소, 배송중, 배송완료인지 검사하여 주문을 수정할 수 있는지 리턴
    - 인자: orderId: 주문ID
    - 반환: ResultVO-주문수정 가능여부, 결과메시지, 배송정보 JSON문자열
    */
    private ResultVO<String> validateOrderUpdatableByDeliveryStatus(String orderId) {
        log.info("Executing <validateOrderUpdatableByDeliveryStatus> for Order Id:{}", orderId);

        ResultVO<String> retVo = new ResultVO<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        DeliveryDTO delivery = queryGateway.query(Constants.QUERY_REPORT, orderId,
                ResponseTypes.instanceOf(DeliveryDTO.class)).join();
        if(delivery == null) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("주문ID <"+orderId+">에 대한 배송정보를 찾을 수 없음");
            return retVo;
        }
        if(!DeliveryStatusEnum.CREATED.value().equals(delivery.getDeliveryStatus())) {
            String msg = DeliveryStatusEnum.DELIVERING.description(delivery.getDeliveryStatus());

            retVo.setReturnCode(false);
            retVo.setReturnMessage("배송상태:"+msg);
            retVo.setResult(gson.toJson(delivery));
            return retVo;
        }
        retVo.setReturnCode(true);
        retVo.setResult(gson.toJson(delivery));
        return retVo;
    }

    /*
    - 기능: 현재 주문 제품과 수정할 주문 제품이 일치하는지 검사
    - 인자: 주문ID
    - 반환: ResultVO-주문수정 가능여부, 결과메시지, 현재 주문 정보 상세정보 JSON 문자열
    */
    private ResultVO<String> validateOrderUpdatableByProduct(OrderReqUpdateDTO orderReqUpdateDTO) {
        final String orderId = orderReqUpdateDTO.getOrderId();
        log.info("Executing <validateOrderUpdatableByProduct> for Order Id:{}", orderId);

        ResultVO<String> retVo = new ResultVO<>();
        Gson gson = new GsonBuilder().create();
        boolean findFlag = true;

        Optional<Order> optOrder = orderRepository.findById(orderId);
        if(optOrder.isPresent()) {
            Order order = optOrder.get();
            OrderDetail orderDetail;
            for(OrderReqDetailDTO item:orderReqUpdateDTO.getOrderReqDetails()) {
                orderDetail = order.getOrderDetails().stream()
                        .filter(detail -> detail.getOrderDetailIdentity().getProductId().equals(item.getProductId()))
                        .findFirst()
                        .orElse(null);

                if(orderDetail == null) {
                    findFlag = false;
                    retVo.setReturnMessage("Can't find Product Id <"+item.getProductId()+"> in current Order");
                    break;
                }
            }
            retVo.setReturnCode(findFlag);
            retVo.setResult(gson.toJson(order.getOrderDetails()));
        } else {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("Can't find Order for " + orderId);
        }

        return retVo;
    }
}
