package com.agilemall.order.events;

import com.agilemall.common.dto.OrderStatus;
import com.agilemall.common.events.OrderCancelledEvent;
import com.agilemall.common.events.OrderCompletedEvent;
import com.agilemall.order.dto.OrderDetailDTO;
import com.agilemall.order.entity.Order;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.entity.OrderDetailIdentity;
import com.agilemall.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OrderEventsHandler {
    @Autowired
    private OrderRepository orderRepository;

    @EventHandler
    public void on(OrderCreatedEvent event) {
        log.info("[@EventHandler] Executing on OrderCreatedEvent");
        List<OrderDetail> newOrderDetails = new ArrayList<>();

        Order order = new Order();
        order.setOrderId(event.getOrderId());
        order.setUserId(event.getUserId());
        order.setOrderStatus(OrderStatus.CREATED.value());
        order.setTotalOrderAmt(event.getTotalOrderAmt());

        for(OrderDetailDTO orderDetail:event.getOrderDetails()) {
            OrderDetailIdentity newOrderDetailIdentity = new OrderDetailIdentity(orderDetail.getOrderId(), orderDetail.getOrderSeq());
            OrderDetail newOrderDetail = new OrderDetail();
            newOrderDetail.setOrderDetailIdentity(newOrderDetailIdentity);
            newOrderDetail.setProductId(orderDetail.getProductId());
            newOrderDetail.setQty(orderDetail.getQty());
            newOrderDetail.setOrderAmt(newOrderDetail.getOrderAmt());

            newOrderDetails.add(newOrderDetail);
        }
        order.setOrderDetails(newOrderDetails);
        orderRepository.save(order);
    }

    @EventHandler
    public void on(OrderCompletedEvent event) {
        log.info("[@EventHandler] Executing on OrderCompletedEvent for Order Id:{}", event.getOrderId());

        //Get order info
        Order order = orderRepository.findById(event.getOrderId()).get();

        Order newOrder = new Order();
        newOrder.setOrderId(event.getOrderId());
        newOrder.setUserId(order.getUserId());
        newOrder.setTotalOrderAmt(order.getTotalOrderAmt());
        newOrder.setOrderStatus(OrderStatus.APPROVED.value());

        orderRepository.save(newOrder);
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        log.info("[@EventHandler] Executing OrderCancelledEvent in OrderEventHandler");
        Order order = (Order) orderRepository.findById(event.getOrderId()).get();
        order.setOrderStatus(event.getOrderStatus());

        orderRepository.save(order);
    }
}
