package com.agilemall.order.query;

import com.agilemall.common.config.Constants;
import com.agilemall.common.dto.OrderDTO;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.order.entity.Order;
import com.agilemall.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderQueryHandler {
    @Autowired
    private OrderRepository orderRepository;

    @QueryHandler(queryName = Constants.QUERY_ORDER_DETAIL)
    public List<OrderDetailDTO> handleOrderDetailQuery(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Constants.QUERY_ORDER_DETAIL,orderId);
        Optional<Order> optOrder = orderRepository.findById(orderId);
        if(optOrder.isPresent()) {
            Order order = optOrder.get();
            List<OrderDetailDTO> orderDetails = order.getOrderDetails().stream()
                    .map(o -> new OrderDetailDTO(order.getOrderId(), o.getProductId(), 0, o.getQty(), o.getOrderAmt()))
                    .collect(Collectors.toList());
            return orderDetails;
        } else {
            return null;
        }
    }

    @QueryHandler(queryName = Constants.QUERY_REPORT)
    public OrderDTO handleReportQuery(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Constants.QUERY_REPORT,orderId);

        Optional<Order> optOrder = orderRepository.findById(orderId);
        if(optOrder.isPresent()) {
            Order order = optOrder.get();
            OrderDTO orderDTO = new OrderDTO();
            BeanUtils.copyProperties(order, orderDTO);
            List<OrderDetailDTO> newOrderDetails = order.getOrderDetails().stream()
                    .map(o -> new OrderDetailDTO(orderId, o.getProductId(), o.getOrderDetailIdentity().getOrderSeq(), o.getQty(), o.getOrderAmt()))
                    .collect(Collectors.toList());
            orderDTO.setOrderDetails(newOrderDetails);
            return orderDTO;
        } else {
            return null;
        }
    }
}
