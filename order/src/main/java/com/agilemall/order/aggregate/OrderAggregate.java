package com.agilemall.order.aggregate;

/*
- 목적: 내/외부로 부터의 요청(Command)을 받는 Aggregate
- 설명
  - 해당 서비스로 들어오는 요청(Command)에 따라 적절한 Command Handler가 실행됨
  - handler는 Aggregate의 최종 상태(@AggregateIdentifier, @AggregateMember로 정의된 항목의 최종 데이터)를 구함
    - Axon Server의 Event저장소에 기록된 Event들을 Replay함
    - 저장된 Event의 종류에 따라 적절한 @EventSoucingHandler가 실햏되면서 Event Replay를 함
    - 성능 향상을 위해 일정 갯수까지 Replay한 결과를 저장한 Snapshot을 이용함
      이를 위해 @Aggregate어노테이션의 속성으로 Snapshot정의와 Cache설정을 정의함
      이러한 설정은 package '*.config > AxonConfig'에 있음
  - Command Handler는 새로운 Event를 생성함
  - 해당 Command를 위한 @EventSourcingHandler가 실행되어 Event Store에 새로운 Event가 등록됨
  - EventHandler class가 생성된 Event에 따라 적절한 처리를 수행
    - Event Handler는 package '*.events'의 class중 'EventHandler'로 끝나는 class임
    - DB에 데이터를 CRUD하는 처리를 수행함
*/

import com.agilemall.common.command.create.CancelCreateOrderCommand;
import com.agilemall.common.dto.OrderDTO;
import com.agilemall.common.dto.OrderDetailDTO;
import com.agilemall.common.dto.OrderStatusEnum;
import com.agilemall.common.events.delete.DeletedDeliveryEvent;
import com.agilemall.order.command.*;
import com.agilemall.order.entity.OrderDetail;
import com.agilemall.order.entity.OrderDetailIdentity;
import com.agilemall.order.events.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.AggregateMember;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache="snapshotCache")
//@Aggregate
public class OrderAggregate {
    @AggregateIdentifier        //각 Aggregate 객체를 구별하는 유일한 필드
    private String orderId;
    @AggregateMember            //Aggregate의 멤버 필드 표시
    private String userId;
    @AggregateMember
    private LocalDateTime orderDatetime;
    @AggregateMember
    private String orderStatus;
    @AggregateMember
    private int totalOrderAmt;

    /*
    복수값 멤버 필드는 각 값을 구별할 수 있는 구별자를 지정해야 함
    package '*.entity > OrderDTO'에 @EntityId 어노테이션으로 지정함
    */
    @AggregateMember
    private List<OrderDetail> orderDetails;

    /* Aggregate객체를 저장
    - Update 트랜잭션 실패 시 이전의 Aggregate객체로 되돌리기 위해 이전 Aggregate정보를 담음
    */
    private final List<OrderDTO> aggregateHistory = new ArrayList<>();

    @Autowired
    private transient CommandGateway commandGateway;

    //Axon framework동작을 위해 비어있는 생성자는 반드시 있어야 함
    public OrderAggregate() {

    }

    //============== START:신규 주문 Command 처리 ===================
    /*
    - 목적: 신규 주문 처리를 위한 사전수행/처리 이벤트 생성
    - 인자: 신규 주문정보를 담고 있는 Command 객체
    - 설명:
      - 사전처리는 OrderService(*.service > OrderService)에서 이미 수행
      - 신규 주문 처리 요청 Event를 생성함
    - 중요: Aggregate객체를 생성하는 Command는 class 생성자에서 처리해야 함. 나머지 Command는 handle 메소드에서 처리
    */
    @CommandHandler
    private OrderAggregate(CreateOrderCommand createOrderCommand) {
        log.info("[@CommandHandler] Executing <CreateOrderCommand> for Order Id: {}", createOrderCommand.getOrderId());

        CreatedOrderEvent createdOrderEvent = new CreatedOrderEvent();
        createdOrderEvent.setOrderId(createOrderCommand.getOrderId());
        createdOrderEvent.setUserId(createOrderCommand.getUserId());
        createdOrderEvent.setOrderDatetime(createOrderCommand.getOrderDatetime());
        createdOrderEvent.setOrderStatus(createOrderCommand.getOrderStatus());
        createdOrderEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        createdOrderEvent.setOrderDetails(createOrderCommand.getOrderDetails());
        createdOrderEvent.setPaymentId(createOrderCommand.getPaymentId());
        createdOrderEvent.setPaymentDetails(createOrderCommand.getPaymentDetails());
        createdOrderEvent.setTotalOrderAmt(createOrderCommand.getTotalOrderAmt());
        createdOrderEvent.setTotalPaymentAmt(createOrderCommand.getTotalPaymentAmt());

        AggregateLifecycle.apply(createdOrderEvent);
    }

    /*
    - 목적: Event저장소에 새로운 Event를 생성함
    */
    @EventSourcingHandler
    private void on(CreatedOrderEvent createdOrderEvent) {
        log.info("[@EventSourcingHandler] Executing <CreatedOrderEvent> for Order Id: {}", createdOrderEvent.getOrderId());

        this.orderId = createdOrderEvent.getOrderId();
        this.userId = createdOrderEvent.getUserId();
        this.orderDatetime = createdOrderEvent.getOrderDatetime();
        this.orderStatus = createdOrderEvent.getOrderStatus();
        this.orderDetails = createdOrderEvent.getOrderDetails().stream()
                .map(o -> new OrderDetail((new OrderDetailIdentity(this.orderId, o.getProductId())), o.getQty(), o.getOrderAmt()))
                .collect(Collectors.toList());
        this.totalOrderAmt = createdOrderEvent.getTotalOrderAmt();
    }

    /*
    - 목적: 신규주문 최종 처리를 위한 사전수행/처리 이벤트 생성
    - 설명
      - 주문 생성/수정/삭제는 Saga패턴이 적용된 Transaction들로 처리됨
      - 본 Command는 Saga 프로세스의 가장 마지막에 요청됨
    */
    @CommandHandler
    private void handle(CompleteOrderCreateCommand completeOrderCreateCommand) throws RuntimeException {
        log.info("[@CommandHandler] Executing <CompleteOrderCreateCommand> for Order Id: {}", completeOrderCreateCommand.getOrderId());

        if("".equals(completeOrderCreateCommand.getOrderId())) {
            throw new RuntimeException("Order Id is MUST NULL");
        }
        CompletedCreateOrderEvent completedCreateOrderEvent = new CompletedCreateOrderEvent();
        BeanUtils.copyProperties(completeOrderCreateCommand, completedCreateOrderEvent);

        AggregateLifecycle.apply(completedCreateOrderEvent);
    }
    @EventSourcingHandler
    private void on(CompletedCreateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CompletedCreateOrderEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();
    }

    /*
    - 목적: 신규주문 처리 실패 처리를 위한 사전수행/처리 이벤트 생성
    */
    @CommandHandler
    private void handle(CancelCreateOrderCommand cancelCreateOrderCommand) {
        log.info("[@CommandHandler] Executing <CancelCreateOrderCommand> for Order Id: {}", cancelCreateOrderCommand.getOrderId());

        CancelledCreateOrderEvent cancelledCreateOrderEvent = new CancelledCreateOrderEvent();
        BeanUtils.copyProperties(cancelCreateOrderCommand, cancelledCreateOrderEvent);

        AggregateLifecycle.apply(cancelledCreateOrderEvent);
    }

    @EventSourcingHandler
    private void on(CancelledCreateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledCreateOrderEvent> for Order Id: {}", event.getOrderId());
        this.orderStatus = event.getOrderStatus();
    }

    //============== END:신규 주문 Command 처리 ===================

    //============== START:주문 수정 Command 처리 ===================
    /*
    - 목적: 주문수정 처리를 위한 사전수행/처리 이벤트 생성
    */
    @CommandHandler
    private void handle(UpdateOrderCommand updateOrderCommand) {
        log.info("[@CommandHandler] Executing <UpdateOrderCommand> for Order Id: {}", updateOrderCommand.getOrderId());

        UpdatedOrderEvent updatedOrderEvent = new UpdatedOrderEvent();
        BeanUtils.copyProperties(updateOrderCommand, updatedOrderEvent);

        AggregateLifecycle.apply(updatedOrderEvent);
    }

    @EventSourcingHandler
    private void on(UpdatedOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <UpdatedOrderEvent> for Order Id: {}", event.getOrderId());

        //-- 수정 또는 삭제 실패 시 이전 정보로 rollback시에만 사용되므로 바로 이전 정보만 담고 있으면 됨
        this.aggregateHistory.clear();
        this.aggregateHistory.add(cloneAggregate(this));    //보상처리를 위해 이전 정보 저장

        this.orderDatetime = event.getOrderDatetime();
        this.orderStatus = OrderStatusEnum.UPTATED.value();
        this.totalOrderAmt = event.getTotalOrderAmt();
        List<OrderDetail> orderDetails = this.orderDetails.stream().toList();
        this.orderDetails.clear();
        for(OrderDetail item:orderDetails) {
            Optional<OrderDetailDTO> optDetail = event.getOrderDetails().stream()
                    .filter(o -> o.getProductId().equals(item.getOrderDetailIdentity().getProductId()))
                    .findFirst();
            if(optDetail.isPresent()) {
                item.setQty(optDetail.get().getQty());
                item.setOrderAmt(optDetail.get().getOrderAmt());
            }
            this.orderDetails.add(item);
        }
    }

    /*
    주문 수정 최종 처리를 위한 사전수행/처리 이벤트 생성
    */
    @CommandHandler
    private void on(CompleteUpdateOrderCommand cmd) {
        log.info("[@CommandHandler] Executing <CompleteUpdateOrderCommand> for Order Id: {}", cmd.getOrderId());

        CompletedUpdateOrderEvent event = new CompletedUpdateOrderEvent();
        event.setOrderId(cmd.getOrderId());
        event.setOrderStatus(cmd.getOrderStatus());

        AggregateLifecycle.apply(event);
    }

    @EventSourcingHandler
    private void handle(CompletedUpdateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CompletedUpdateOrderEvent> for Order Id: {}", event.getOrderId());

        this.orderStatus = event.getOrderStatus();

    }

    /*
    - 목적: 주문 수정 처리 취소를 위한 사전수행/처리 이벤트 생성
    - 설명
      - 수정 이전의 상태로 돌리기 위해 aggregateHistory에 저장된 이전 Aggregate객체를 이용
      - 이전 Aggregate객체 정보를 읽어 주문 수정 Command를 다시 보냄
        이때 이 요청은 보상처리 Command임을 나타내는 isCompensation을 true로 설정함
        이 정보는 보상 트랜잭션 실패 시 무한 루프 방지를 위해 이용됨(*.saga > OrderUpdatingSaga: UpdatedOrderEvent참조)
    */
    @CommandHandler
    private void handle(CancelUpdateOrderCommand cancelUpdateOrderCommand) {
        log.info("[@CommandHandler] Executing <CancelUpdateOrderCommand> for Order Id: {}", cancelUpdateOrderCommand.getOrderId());

        CancelledUpdateOrderEvent cancelledUpdateOrderEvent = new CancelledUpdateOrderEvent();
        cancelledUpdateOrderEvent.setOrderId(cancelUpdateOrderCommand.getOrderId());

        AggregateLifecycle.apply(cancelledUpdateOrderEvent);

        //-- send UpdateOrderCommand to compensate
        if(this.aggregateHistory.isEmpty()) return;

        OrderDTO order = this.aggregateHistory.get(this.aggregateHistory.size() - 1);
        UpdateOrderCommand cmd = UpdateOrderCommand.builder()
                .orderId(order.getOrderId())
                .orderDatetime(order.getOrderDatetime())
                .totalOrderAmt(order.getTotalOrderAmt())
                .orderDetails(order.getOrderDetails())
                .isCompensation(cancelUpdateOrderCommand.isCompensation())           //보상처리 Command 표시
                .build();

        //불필요한 메모리 사용 방지를 위해 초기화함
        //@EventSourcingHandler on <UpdatedOrderEvent>에서 다시 적재됨

        commandGateway.send(cmd);
    }

    @EventSourcingHandler
    private void on(CancelledUpdateOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledUpdateOrderEvent> for Order Id: {}", event.getOrderId());

    }
    //============== END:주문 수정 Command 처리 ===================

    //============== START:주문 취소 Command 처리 ===================
    @CommandHandler
    private void handle(DeleteOrderCommand deleteOrderCommand) {
        log.info("[@EventSourcingHandler] Executing <DeleteOrderCommand> for Order Id: {}", deleteOrderCommand.getOrderId());
        AggregateLifecycle.apply(new DeletedOrderEvent(deleteOrderCommand.getOrderId()));
    }
    @EventSourcingHandler
    private void on(DeletedDeliveryEvent event) {
        log.info("[@EventSourcingHandler] Executing <DeletedDeliveryEvent> for Order Id: {}", event.getOrderId());
        this.orderStatus = OrderStatusEnum.ORDER_CANCLLED.value();
    }

    @CommandHandler
    private void handle(CompleteDeleteOrderCommand completeDeleteOrderCommand) {
        log.info("[@EventSourcingHandler] Executing <CompleteDeleteOrderCommand> for Order Id: {}", completeDeleteOrderCommand.getOrderId());
        AggregateLifecycle.apply(new CompletedDeleteOrderEvent(completeDeleteOrderCommand.getOrderId()));
    }
    @EventSourcingHandler
    private void on(CompletedDeleteOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CompletedDeleteOrderEvent> for Order Id: {}", event.getOrderId());

    }

    @CommandHandler
    private void handle(CancelDeleteOrderCommand cancelDeleteOrderCommand) {
        log.info("[@EventSourcingHandler] Executing <CancelDeleteOrderCommand> for Order Id: {}", cancelDeleteOrderCommand.getOrderId());
        AggregateLifecycle.apply(new CancelledDeleteOrderEvent(cancelDeleteOrderCommand.getOrderId()));
    }
    @EventSourcingHandler
    private void on(CancelledDeleteOrderEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledDeleteOrderEvent> for Order Id: {}", event.getOrderId());
        this.orderStatus = OrderStatusEnum.COMPLETED.value();
    }
    //============== END:주문 취소 Command 처리 ===================

    /*
    Aggregate객체의 정보를 OrderDTO 객체로 변환하여 복사
    */
    private OrderDTO cloneAggregate(OrderAggregate orderAggregate) {
        OrderDTO order = new OrderDTO();
        order.setOrderId(orderAggregate.orderId);
        order.setUserId(orderAggregate.userId);
        order.setOrderDatetime(orderAggregate.orderDatetime);
        order.setOrderStatus(orderAggregate.orderStatus);
        order.setOrderDetails(
                orderAggregate.orderDetails.stream()
                        .map(o -> new OrderDetailDTO(
                                order.getOrderId(), o.getOrderDetailIdentity().getProductId(),
                                o.getQty(), o.getOrderAmt()))
                        .collect(Collectors.toList()));
        return order;
    }
}
