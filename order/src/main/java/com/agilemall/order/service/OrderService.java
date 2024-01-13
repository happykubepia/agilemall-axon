package com.agilemall.order.service;
/*
- 목적: OrderController에서 호출되어 Order service에 대한 처리를 한 후 이후 처리는 Aggregate의 Command Handler에 요청함
*/
import com.agilemall.order.command.DeleteOrderCommand;
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

    /*
    - 목적: 신규 주문 처리를 진행하기 위한 validation체크 후 주문생성 Command객체를 생성하여 이후 처리를 요청함
    */
    public ResultVO<CreateOrderCommand> createOrder(OrderReqCreateDTO orderReqCreateDTO) {
        log.info("[OrderService] Executing <createOrder>: {}", orderReqCreateDTO.toString());
        log.info("===== [Create Order] START Transaction =====");

        ResultVO<CreateOrderCommand> retVo = new ResultVO<>();

        //-- 결제수단과 결제 비율 입력값 유효성 체크: 결제 수단은 10번 또는 20번이어야 하고, 결제 비율합은 1이어야 함
        List<PaymentReqDetailDTO> payDetails = orderReqCreateDTO.getPaymentReqDetails();
        ResultVO<String> retPay = isValidPaymentInput(payDetails);
        if (!retPay.isReturnCode()) {
            log.info(retPay.getReturnMessage());
            retVo.setReturnCode(retPay.isReturnCode());
            retVo.setReturnMessage(retPay.getReturnMessage());
            return retVo;
        }

        //--제품 재고 정보를 Query하여 재고 여부를 검사
        log.info("===== [Create Order] #1: <isValidInventory> =====");
        List<ResultVO<InventoryDTO>> inventories = getInventory(orderReqCreateDTO.getOrderReqDetails());
        String retCheck = isValidInventory(inventories);
        if (!retCheck.isEmpty()) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck);
            return retVo;
        }

        //-- 주문ID, 결제ID를 채번하고 요청 정보에서 사용자ID를 읽음
        String orderId = "ORDER_" + RandomStringUtils.random(9, false, true);
        String paymentId = "PAY_" + RandomStringUtils.random(11, false, true);
        String userId = orderReqCreateDTO.getUserId();

        //주문상세 정보 객체 생성
        List<OrderDetailDTO> newOrderDetails = orderReqCreateDTO.getOrderReqDetails().stream()
                .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getQty(), o.getQty() * getUnitPrice(inventories, o.getProductId())))
                .collect(Collectors.toList());
        //*참고)Stream: https://futurecreator.github.io/2018/08/26/java-8-streams/

        //주문금액합계
        int totalOrderAmt = newOrderDetails.stream().mapToInt(o->o.getOrderAmt()).sum();

        //결제정보 설정
        List<PaymentDetailDTO> newPaymentDetails = orderReqCreateDTO.getPaymentReqDetails().stream()
                .map(p -> new PaymentDetailDTO(orderId, paymentId, p.getPaymentKind(), (int) Math.round(p.getPaymentRate() * totalOrderAmt)))
                .collect(Collectors.toList());

        //결제금액 합계
        int totalPaymentAmt = newPaymentDetails.stream().mapToInt(o->o.getPaymentAmt()).sum();

        //--Command객체 생성
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

        try {
            log.info("===== [Create Order] #2: <CreateOrderCommand> =====");

            //-- Command객체를 Axon서버로 발송. Axon서버는 Command Handler가 있는 서비스의 Aggregate로 메시지 전달함
            commandGateway.sendAndWait(createOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Order Created");
            retVo.setResult(createOrderCommand);
        } catch (Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }

    /*
    - 목적: 주문 수정 처리를 위한 validation 체크 후 이후 처리 요청을 위한 Command객체 발송
    */
    public ResultVO<UpdateOrderCommand> updateOrder(OrderReqUpdateDTO orderReqUpdateDTO) {
        log.info("[OrderService] Executing <updateOrder>: {}", orderReqUpdateDTO.toString());
        log.info("===== [Update Order] START Transaction =====");

        //-- 요청된 주문ID에 해당하는 주분정보를 구함
        String orderId = orderReqUpdateDTO.getOrderId();
        ResultVO<String> retCheck;
        ResultVO<UpdateOrderCommand> retVo = new ResultVO<>();
        Optional<Order> optOrder = orderRepository.findById(orderId);
        if (optOrder.isEmpty()) {
            log.info("Can't find Order info for Order Id: {}", orderId);
            retVo = new ResultVO<>();
            retVo.setReturnCode(false);
            retVo.setReturnMessage("Can't find Order info for Order Id:"+orderId);
            return retVo;
        }

        Order order = optOrder.get();

        //-- 요청 제품코드 유효성 체크: 주문하지도 않은 제품을 수정하려 하는지 검사
        retCheck = isValidateProductInput(order, orderReqUpdateDTO);
        if (!retCheck.isReturnCode()) {
            log.info("NOT Updatable: {}", retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        //-- 결제수단과 결제비율 입력값 유효성 체크
        List<PaymentReqDetailDTO> payDetails = orderReqUpdateDTO.getPaymentReqDetails();
        retCheck = isValidPaymentInput(payDetails);
        if (!retCheck.isReturnCode()) {
            log.info(retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        retVo = new ResultVO<>();

        //-- 배송상태를 읽어 주문 수정 가능한지 검사. 배송상태가 'CREATED'인 경우만 주문 수정 가능함.
        log.info("===== [Update Order] #1: <validateOrderUpdatableByDeliveryStatus> =====");
        retCheck = validateOrderUpdatableByDeliveryStatus(orderId);
        if (!retCheck.isReturnCode()) {
            log.info("NOT Updatable: {}", retCheck.getReturnMessage());
            retVo.setReturnCode(false);
            retVo.setReturnMessage(retCheck.getReturnMessage());
            return retVo;
        }

        //-- 주문 제품의 재고상태를 읽어 주문 수정 가능한지 검사
        log.info("===== [Update Order] #2: <isValidInventory> =====");
        List<ResultVO<InventoryDTO>> inventories = getInventory(orderReqUpdateDTO.getOrderReqDetails());
        String strCheck = isValidInventory(inventories);
        if (!strCheck.isEmpty()) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(strCheck);
            return retVo;
        }

        log.info("===== [Update Order] #3: <UpdateOrderCommand> =====");

        //-- 수정 요청할 주문상세 정보 구함: 기존 주문상세 정보 리스트에 변경 요청 정보를 반영하여 구함
        List<OrderDetailDTO> newOrderDetails = new ArrayList<>();
        OrderDetailDTO newOrderDetail;
        OrderReqDetailDTO reqDetail;
        for(OrderDetail item:order.getOrderDetails()) {
            Optional<OrderReqDetailDTO> optObj = orderReqUpdateDTO.getOrderReqDetails().stream()
                    .filter(o -> o.getProductId().equals(item.getOrderDetailIdentity().getProductId()))
                    .findFirst();
            if(optObj.isPresent()) {    //변경된 주문 상세 정보 반영
                reqDetail = optObj.get();
                newOrderDetail = new OrderDetailDTO(orderId, reqDetail.getProductId(),
                        reqDetail.getQty(), reqDetail.getQty() * getUnitPrice(inventories, reqDetail.getProductId()));
            } else {                    //변경 안된 주문 상세 정보는 그대로 유지
                newOrderDetail = new OrderDetailDTO(orderId, item.getOrderDetailIdentity().getProductId(),
                        item.getQty(), item.getOrderAmt());
            }
            newOrderDetails.add(newOrderDetail);
        }

        //주문금액합계 계산
        int totalOrderAmt = newOrderDetails.stream().mapToInt(OrderDetailDTO::getOrderAmt).sum();

        //결제 상세정보 재설정
        PaymentDTO payment = queryGateway.query(Constants.QUERY_REPORT, orderId,
                ResponseTypes.instanceOf(PaymentDTO.class)).join();
        if (payment == null) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("주문ID <" + orderId + ">에 대한 결제정보를 찾을 수 없음");
            return retVo;
        }

        List<PaymentDetailDTO> newPaymentDetails = orderReqUpdateDTO.getPaymentReqDetails().stream()
                .map(p -> new PaymentDetailDTO(orderId, payment.getPaymentId(), p.getPaymentKind(), (int) Math.round(p.getPaymentRate() * totalOrderAmt)))
                .collect(Collectors.toList());

        //결제금액 합계
        int totalPaymentAmt = newPaymentDetails.stream().mapToInt(PaymentDetailDTO::getPaymentAmt).sum();

        //주문 수정 Command 메시지 생성
        UpdateOrderCommand updateOrderCommand = UpdateOrderCommand.builder()
                .orderId(orderReqUpdateDTO.getOrderId())
                .orderDatetime(LocalDateTime.now())
                .totalOrderAmt(totalOrderAmt)
                .orderDetails(newOrderDetails)
                .paymentId(payment.getPaymentId())
                .paymentDetails(newPaymentDetails)
                .totalPaymentAmt(totalPaymentAmt)
                .build();

        //-- 주문 수정 Command 발송
        try {
            commandGateway.sendAndWait(updateOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            //commandGateway.send(updateOrderCommand);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Success to request <UpdateOrderCommand>");
            retVo.setResult(updateOrderCommand);

        } catch (Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }

    /*
    - 목적: 주문 취소 가능한 지 Validation 체크 후 주문 취소 Command 메시지 발송
    */
    public ResultVO<String> deleteOrder(String orderId) {
        log.info("[OrderService] Executing <deleteOrder>: {}", orderId);
        log.info("===== [Delete Order] START Transaction =====");

        //-- 현재 배송상태를 읽어 주문 삭제 가능한지 검사. 주문 상태가 'CREATED'인 경우만 취소 가능함.
        log.info("===== [Delete Order] #1: <validateOrderDeletableByDeliveryStatus> =====");
        ResultVO<String> retCheck = new ResultVO<>();
        retCheck = validateOrderDeletableByDeliveryStatus(orderId);
        if (!retCheck.isReturnCode()) {
            log.info("NOT Deletable: {}", retCheck.getReturnMessage());
            return retCheck;
        }

        ResultVO<String> retVo = new ResultVO<>();

        //-- 주문 취소 Command 발송
        DeleteOrderCommand deleteOrderCommand = DeleteOrderCommand.builder().orderId(orderId).build();
        try {
            commandGateway.sendAndWait(deleteOrderCommand, Constants.GATEWAY_TIMEOUT, TimeUnit.SECONDS);
            retVo.setReturnCode(true);
            retVo.setReturnMessage("Success to delete for Order Id:" + orderId);
        } catch (Exception e) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage(e.getMessage());
        }
        return retVo;
    }

    //=============== Private Method =======================

    //-- 주문 상세 정보에 있는 각 제품 객체를 리턴함
    private List<ResultVO<InventoryDTO>> getInventory(List<OrderReqDetailDTO> orderDetails) {
        log.info("[OrderService] Executing getInventory");

        GetInventoryByProductIdQuery getInventoryByProductIdQuery;
        List<ResultVO<InventoryDTO>> inventories = new ArrayList<>();
        ResultVO<InventoryDTO> retVo;
        int reqQty;

        InventoryDTO inventoryDTO;
        try {
            for (OrderReqDetailDTO orderDetail : orderDetails) {
                //QueryGateway로 Query객체와 응답객체 형식을 발송함. Axon서버는 Query Handler가 있는 서비스를 호출하여 결과를 리턴함
                getInventoryByProductIdQuery = new GetInventoryByProductIdQuery(orderDetail.getProductId());
                inventoryDTO = queryGateway.query(getInventoryByProductIdQuery, ResponseTypes.instanceOf(InventoryDTO.class)).join();

                reqQty = orderDetail.getQty();
                retVo = new ResultVO<>();
                retVo.setResult(inventoryDTO);
                retVo.setReturnCode(reqQty <= inventoryDTO.getInventoryQty() && inventoryDTO.getInventoryQty() != 0);
                inventories.add(retVo);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return inventories;
    }

    //-- 주문 수정 요청의 validation 체크: 등록된 결제 수단(카드 또는 포인트)인지와 결제 비율의 합이 1인지 검사
    private ResultVO<String> isValidPaymentInput(List<PaymentReqDetailDTO> paymentDetails) {
        ResultVO<String> retVo = new ResultVO<>();
        //지불수단 유효성 체크
        for (PaymentReqDetailDTO item : paymentDetails) {
            Optional<PaymentKindEnum> optEnum = Arrays.stream(PaymentKindEnum.values())
                    .filter(o -> o.value().equals(item.getPaymentKind()))
                    .findFirst();
            if (optEnum.isEmpty()) {
                retVo.setReturnCode(false);
                retVo.setReturnMessage("지불수단 <" + item.getPaymentKind() + ">은 정상적인 지불 수단 아님");
                return retVo;
            }
        }

        //결제 비율의 합이 1인지 체크
        if (paymentDetails.stream().mapToDouble(PaymentReqDetailDTO::getPaymentRate).sum() != 1) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("결제 비율의 합은 1이어야 함");
            return retVo;
        }

        retVo.setReturnCode(true);
        retVo.setReturnMessage("유효한 결제수단 및 결제비율임");
        return retVo;
    }

    //-- 상품 재고가 있는지 검사하여 리턴
    private String isValidInventory(List<ResultVO<InventoryDTO>> inventories) {
        log.info("[OrderService] Executing <isValidInventory>");

        for (ResultVO<InventoryDTO> retVo : inventories) {
            if (!retVo.isReturnCode()) {
                return "재고없음: " + retVo.getResult().getProductId();
            }
        }
        return "";
    }

    //-- 제품ID에 해당하는 제품단가를 리턴
    private int getUnitPrice(List<ResultVO<InventoryDTO>> inventories, String productId) {
        log.info("[OrderService] Executing <getUnitPrice>");

        Optional<ResultVO<InventoryDTO>> optInventory = inventories.stream()
                .filter(obj -> productId.equals(obj.getResult().getProductId()))
                .findFirst();
        return (optInventory.map(inventoryDTOResultVO -> inventoryDTOResultVO.getResult().getUnitPrice()).orElse(0));
    }

    //--배송상태를 읽어 주문을 수정할 수 있는지 리턴
    private ResultVO<String> validateOrderUpdatableByDeliveryStatus(String orderId) {
        log.info("Executing <validateOrderUpdatableByDeliveryStatus> for Order Id:{}", orderId);

        ResultVO<String> retVo = new ResultVO<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        DeliveryDTO delivery = queryGateway.query(Constants.QUERY_REPORT, orderId,
                ResponseTypes.instanceOf(DeliveryDTO.class)).join();
        if (delivery == null) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("주문ID <" + orderId + ">에 대한 배송정보를 찾을 수 없음");
            return retVo;
        }
        if (!DeliveryStatusEnum.CREATED.value().equals(delivery.getDeliveryStatus())) {
            String msg = DeliveryStatusEnum.DELIVERING.description(delivery.getDeliveryStatus());

            retVo.setReturnCode(false);
            retVo.setReturnMessage("배송상태:" + msg);
            retVo.setResult(gson.toJson(delivery));
            return retVo;
        }
        retVo.setReturnCode(true);
        retVo.setResult(gson.toJson(delivery));
        return retVo;
    }

    //-- 요청 제품코드 유효성 체크-주문하지도 않은 제품을 수정하려 하는지 검사
    private ResultVO<String> isValidateProductInput(Order order, OrderReqUpdateDTO orderReqUpdateDTO) {
        final String orderId = orderReqUpdateDTO.getOrderId();
        log.info("Executing <validateOrderUpdatableByProduct> for Order Id:{}", orderId);

        ResultVO<String> retVo = new ResultVO<>();
        Gson gson = new GsonBuilder().create();
        boolean findFlag = true;

        OrderDetail orderDetail;
        for (OrderReqDetailDTO item : orderReqUpdateDTO.getOrderReqDetails()) {
            orderDetail = order.getOrderDetails().stream()
                    .filter(detail -> detail.getOrderDetailIdentity().getProductId().equals(item.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (orderDetail == null) {
                findFlag = false;
                retVo.setReturnMessage("Can't find Product Id <" + item.getProductId() + "> in current Order");
                break;
            }
        }
        retVo.setReturnCode(findFlag);
        retVo.setResult(gson.toJson(order.getOrderDetails()));

        return retVo;
    }

    //-- 배송상태가 생성인지 여부를 검사함. 생성 상태인 경우에만 true를 리턴
    private ResultVO<String> validateOrderDeletableByDeliveryStatus(String orderId) {
        log.info("Executing <validateOrderDeletableByDeliveryStatus> for Order Id:{}", orderId);

        ResultVO<String> retVo = new ResultVO<>();
        retVo.setResult(orderId);
        DeliveryDTO delivery = queryGateway.query(Constants.QUERY_REPORT, orderId,
                ResponseTypes.instanceOf(DeliveryDTO.class)).join();
        if (delivery == null) {
            retVo.setReturnCode(false);
            retVo.setReturnMessage("주문ID <" + orderId + ">에 대한 배송정보를 찾을 수 없음");
            return retVo;
        }
        if (!DeliveryStatusEnum.CREATED.value().equals(delivery.getDeliveryStatus())) {
            String msg = DeliveryStatusEnum.DELIVERING.description(delivery.getDeliveryStatus());

            retVo.setReturnCode(false);
            retVo.setReturnMessage("배송상태:" + msg);
            return retVo;
        }
        retVo.setReturnCode(true);
        return retVo;
    }
}
