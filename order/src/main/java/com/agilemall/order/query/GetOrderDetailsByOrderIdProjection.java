package com.agilemall.order.query;

import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.quries.GetOrderDetailsByOrderIdQuery;
import com.agilemall.order.entity.Order;
import com.agilemall.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class GetOrderDetailsByOrderIdProjection {
    @Autowired
    private OrderRepository orderRepository;

    @QueryHandler
    public List<OrderDetailDTO> getOrderDetails(GetOrderDetailsByOrderIdQuery qry) {
        log.info("[@QueryHandler] getOrderDetails in GetOrderDetailsByOrderIdProjection for Order Id: {}", qry.getOrderId());

        Optional<Order> optOrder = orderRepository.findById(qry.getOrderId());
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
}
