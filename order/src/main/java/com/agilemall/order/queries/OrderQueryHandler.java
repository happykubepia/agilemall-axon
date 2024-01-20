package com.agilemall.order.queries;
/*
- 목적: Quary요청에 대한 수행 및 응답
- 설명
    - Query요청은 1st 인자로 Query명, 2nd 인자로 Query조건, 3rd인자로 응답class형식으로 구성됨
    ReportDTO report = queryGateway.query(Constants.QUERY_REPORT, event.getOrderId(),
                ResponseTypes.instanceOf(ReportDTO.class)).join();
    - @QueryHandler 어노테이션의 queryName과 매칭되는 QueryHandler가 수행됨
*/
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

    private final OrderRepository orderRepository;
    @Autowired
    public OrderQueryHandler(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    //-- 주문ID에 해당하는 주문 상세정보를 리턴
    @QueryHandler(queryName = Constants.QUERY_ORDER_DETAIL)
    private List<OrderDetailDTO> handleOrderDetailQuery(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Constants.QUERY_ORDER_DETAIL,orderId);
        Optional<Order> optOrder = orderRepository.findById(orderId);
        if(optOrder.isPresent()) {
            Order order = optOrder.get();
            return order.getOrderDetails().stream()
                    .map(o -> new OrderDetailDTO(order.getOrderId(), o.getOrderDetailIdentity().getProductId(), o.getQty(), o.getOrderAmt()))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    //-- 주문ID에 해당하는 주문 정보 전체를 리턴
    @QueryHandler(queryName = Constants.QUERY_REPORT)
    private OrderDTO handleReportQuery(String orderId) {
        log.info("[@QueryHandler] Handle <{}> for Order Id: {}", Constants.QUERY_REPORT,orderId);

        Optional<Order> optOrder = orderRepository.findById(orderId);
        if(optOrder.isPresent()) {
            Order order = optOrder.get();
            OrderDTO orderDTO = new OrderDTO();
            BeanUtils.copyProperties(order, orderDTO);
            List<OrderDetailDTO> newOrderDetails = order.getOrderDetails().stream()
                    .map(o -> new OrderDetailDTO(orderId, o.getOrderDetailIdentity().getProductId(), o.getQty(), o.getOrderAmt()))
                    .collect(Collectors.toList());
            orderDTO.setOrderDetails(newOrderDetails);
            return orderDTO;
        } else {
            return null;
        }
    }
}
