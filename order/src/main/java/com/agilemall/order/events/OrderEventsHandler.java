package com.agilemall.order.events;
/*
- 목적: OrderAggregate에서 생성된 Event 처리를 수행
*/
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.order.entity.Order;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.entity.OrderDetailIdentity;
import com.agilemall.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ProcessingGroup("orders")      //전체 Event Replay시 대상 class를 구별하기 위해 부여
@AllowReplay                    //Event Replay를 활성화 함. 비활성화할 EventHandler에는 @DisallowReplay를 지정
public class OrderEventsHandler {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private transient EventGateway eventGateway;

    //==================== 신규 주문 관련 이벤트 처리 ======================
    @EventHandler
    private void on(CreatedOrderEvent event) {
        log.info("[@EventHandler] Handle <CreatedOrderEvent> for Order Id: {}", event.getOrderId());

        List<OrderDetail> newOrderDetails = new ArrayList<>();

        try {
            Order order = new Order();
            order.setOrderId(event.getOrderId());
            order.setUserId(event.getUserId());
            order.setOrderDatetime(event.getOrderDatetime());
            order.setOrderStatus(OrderStatusEnum.CREATED.value());
            order.setTotalOrderAmt(event.getTotalOrderAmt());

            for(OrderDetailDTO orderDetail:event.getOrderDetails()) {
                OrderDetailIdentity newOrderDetailIdentity = new OrderDetailIdentity(orderDetail.getOrderId(), orderDetail.getProductId());
                OrderDetail newOrderDetail = new OrderDetail();
                newOrderDetail.setOrderDetailIdentity(newOrderDetailIdentity);
                newOrderDetail.setQty(orderDetail.getQty());
                newOrderDetail.setOrderAmt(orderDetail.getOrderAmt());

                newOrderDetails.add(newOrderDetail);
            }
            order.setOrderDetails(newOrderDetails);
            orderRepository.save(order);
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedCreateOrderEvent(event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CompletedCreateOrderEvent event) {
        log.info("[@EventHandler] Executing on <CompletedCreateOrderEvent> for Order Id:{}", event.getOrderId());

        try {
            //Get order info
            Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
            if(optOrder.isPresent()) {
                Order order = optOrder.get();
                order.setOrderStatus(event.getOrderStatus());
                orderRepository.save(order);
            } else {
                log.error("Can't get Order for Order Id: {}", event.getOrderId());
                eventGateway.publish(new FailedCompleteCreateOrderEvent(event.getOrderId()));
            }
        } catch(Exception e) {
            log.error("Error is occur during handle <CompletedCreateOrderEvent>: {}", e.getMessage());

            eventGateway.publish(new FailedCompleteCreateOrderEvent(event.getOrderId()));
        }

    }

    @EventHandler
    @DisallowReplay
    private void on(CancelledCreateOrderEvent event) {
        log.info("[@EventHandler] Executing <CancelledCreateOrderEvent> for Order Id: {}", event.getOrderId());
    }

    //==================== 주문 수정 관련 이벤트 처리 ======================
    /*

    - 참고: https://blossoming-man.tistory.com/entry/CascadeTypeREMOVE%EC%99%80-orpahnRemovalTrue
    */
    @EventHandler
    private void on(UpdatedOrderEvent event) {
        log.info("[@EventHandler] Executing <UpdatedOrderEvent> for Order Id: {}", event.getOrderId());
        if(event.isCompensation()) {
            log.info("**** This is Compensation transaction");
        }

        try {
            Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
            if(optOrder.isEmpty()) return;

            Order order = optOrder.get();
            List<OrderDetail> orderDetails = order.getOrderDetails().stream().toList();
            order.getOrderDetails().clear();
            for(OrderDetail item:orderDetails) {
                Optional<OrderDetailDTO> optDetail = event.getOrderDetails().stream()
                        .filter(o -> o.getProductId().equals(item.getOrderDetailIdentity().getProductId()))
                        .findFirst();
                if(optDetail.isPresent()) {
                    item.setQty(optDetail.get().getQty());
                    item.setOrderAmt(optDetail.get().getOrderAmt());
                }
                //log.info(item.getOrderDetailIdentity().getProductId() + "=>" + item.getQty()+", "+ item.getOrderAmt());
                order.getOrderDetails().add(item);
            }
            order.setOrderDetails(order.getOrderDetails());
            order.setTotalOrderAmt(event.getTotalOrderAmt());
            order.setOrderStatus(OrderStatusEnum.UPTATED.value());
            orderRepository.save(order);
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedUpdateOrderEvent(event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CompletedUpdateOrderEvent event) {
        log.info("[@EventHandler] Executing <CompletedUpdateOrderEvent> for Order Id: {}", event.getOrderId());

        try {
            Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
            if(optOrder.isEmpty()) return;

            Order order = optOrder.get();
            order.setOrderStatus(OrderStatusEnum.COMPLETED.value());
            orderRepository.save(order);
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedCompleteUpdateOrderEvent(event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CancelledUpdateOrderEvent event) {
        log.info("[@EventHandler] Executing <CancelledUpdateOrderEvent> for Order Id: {}", event.getOrderId());

    }

    //==================== 주문 삭제 관련 이벤트 처리 ======================
    @EventHandler
    private void on(DeletedOrderEvent event) {
        log.info("[@EventHandler] Executing <DeletedOrderEvent> for Order Id: {}", event.getOrderId());
        Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
        if(optOrder.isPresent()) {
            //orderRepository.delete(optOrder.get());
            optOrder.get().setOrderStatus(OrderStatusEnum.ORDER_CANCLLED.value());
            orderRepository.save(optOrder.get());
        } else {
            log.info("Can't find Order for Order Id:{}", event.getOrderId());
        }
    }

    @EventHandler
    private void on(CompletedDeleteOrderEvent event) {
        log.info("[@EventHandler] Executing <CompletedDeleteOrderEvent> for Order Id: {}", event.getOrderId());

        Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
        if(optOrder.isEmpty()) return;

        try {
           orderRepository.delete(optOrder.get());

        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedCompleteDeleteOrderEvent(event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CancelledDeleteOrderEvent event) {
        log.info("[@EventHandler] Executing <CancelledDeleteOrderEvent> for Order Id: {}", event.getOrderId());

        Optional<Order> optOrder = orderRepository.findById(event.getOrderId());
        if(optOrder.isEmpty()) return;

        try {
            Order order = optOrder.get();
            order.setOrderStatus(OrderStatusEnum.COMPLETED.value());
            orderRepository.save(order);
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }

    //===================== 전체 이벤트 Replay하여 DB에 최종 상태 저장 ===========
    @ResetHandler
    private void replayAll() {
        log.info("[@ResetHandler] Executing replayAll");
        orderRepository.deleteAll();
    }
}

