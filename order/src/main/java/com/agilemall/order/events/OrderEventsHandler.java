package com.agilemall.order.events;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.OrderStatus;
import com.agilemall.common.events.OrderCancelledEvent;
import com.agilemall.common.events.OrderCompletedEvent;
import com.agilemall.common.events.ReportUpdateEvent;
import com.agilemall.order.dto.OrderDetailDTO;
import com.agilemall.order.entity.Order;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.entity.OrderDetailIdentity;
import com.agilemall.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@EnableRetry
public class OrderEventsHandler {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EventGateway eventGateway;

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

        requestSendUpdateReport(event.getOrderId(), newOrder.getOrderStatus());
    }

    @EventHandler
    public void on(OrderCancelledEvent event) {
        log.info("[@EventHandler] Executing OrderCancelledEvent in OrderEventHandler");
        Order order = orderRepository.findById(event.getOrderId()).get();
        order.setOrderStatus(event.getOrderStatus());

        orderRepository.save(order);

        requestSendUpdateReport(event.getOrderId(), event.getOrderStatus());
    }

    //각 서비스에 Report service에 추가/갱신된 정보를 보내도록 Event 전송
    @Retryable(
            maxAttempts = Constants.RETRYABLE_MAXATTEMPTS,
            retryFor = {IOException.class, TimeoutException.class, RuntimeException.class},
            backoff = @Backoff(delay = Constants.RETRYABLE_DELAY)
    )

    public void requestSendUpdateReport(String orderId, String orderStatus) {
        log.info("[@EventHandler] Executing requestSendUpdateReport in OrderEventHandler for order Id: {}", orderId);
        log.info("===== [OrderEventsHandler] Transaction #7: Retryable transaction <requestSendUpdateReport> =====");
        ReportUpdateEvent reportUpdateEvent = ReportUpdateEvent.builder()
                .orderId(orderId)
                .orderStatus(orderStatus)
                .build();

        eventGateway.publish(reportUpdateEvent);
    }
}
