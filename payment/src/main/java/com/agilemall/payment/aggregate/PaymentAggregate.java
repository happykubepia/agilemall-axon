package com.agilemall.payment.aggregate;

import com.agilemall.common.command.create.CancelCreatePaymentCommand;
import com.agilemall.common.command.create.CreatePaymentCommand;
import com.agilemall.common.command.delete.CancelDeletePaymentCommand;
import com.agilemall.common.command.delete.DeletePaymentCommand;
import com.agilemall.common.command.update.CancelUpdatePaymentCommand;
import com.agilemall.common.command.update.UpdatePaymentCommand;
import com.agilemall.common.dto.PaymentDTO;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.events.create.CancelledCreatePaymentEvent;
import com.agilemall.common.events.create.CreatedPaymentEvent;
import com.agilemall.common.events.delete.CancelledDeletePaymentEvent;
import com.agilemall.common.events.delete.DeletedPaymentEvent;
import com.agilemall.common.events.update.CancelledUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.payment.entity.PaymentDetail;
import com.agilemall.payment.entity.PaymentDetailIdentity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
//@Aggregate
@Aggregate(snapshotTriggerDefinition = "snapshotTrigger", cache = "snapshotCache")
public class PaymentAggregate {
    @AggregateIdentifier
    private String paymentId;

    @AggregateMember
    private String orderId;
    @AggregateMember
    private int totalPaymentAmt;
    @AggregateMember
    private String paymentStatus;
    @AggregateMember
    private List<PaymentDetail> paymentDetails;

    private final List<PaymentDTO> aggregateHistory = new ArrayList<>();

    @Autowired
    private transient CommandGateway commandGateway;

    public PaymentAggregate() {

    }

    @CommandHandler
    private PaymentAggregate(CreatePaymentCommand createPaymentCommand) {
        log.info("[@CommandHandler] Executing CreatePaymentCommand..");
        log.info(createPaymentCommand.toString());

        CreatedPaymentEvent createdPaymentEvent = new CreatedPaymentEvent();
        BeanUtils.copyProperties(createPaymentCommand, createdPaymentEvent);

        try {
            AggregateLifecycle.apply(createdPaymentEvent);
            //throw new Exception("Payment service is not working temporary");
        } catch(Exception e) {
            log.error(e.getMessage());
        }
    }

    @EventSourcingHandler
    private void on(CreatedPaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing CreatedPaymentEvent ..");

        this.paymentId = event.getPaymentId();
        this.orderId = event.getOrderId();
        this.totalPaymentAmt = event.getTotalPaymentAmt();
        //this.paymentDetails.clear();
        this.paymentDetails = event.getPaymentDetails().stream()
                .map(o -> new PaymentDetail(
                        (new PaymentDetailIdentity(event.getPaymentId(), o.getPaymentKind())),
                        o.getPaymentAmt())
                ).collect(Collectors.toList());

        this.aggregateHistory.add(cloneAggregate(this));
    }

    @CommandHandler
    private void handle(CancelCreatePaymentCommand cancelCreatePaymentCommand) {
        log.info("[@CommandHandler] Executing CancelCreatePaymentCommand for Order Id: {} and Payment Id: {}",
                cancelCreatePaymentCommand.getOrderId(), cancelCreatePaymentCommand.getPaymentId());

        CancelledCreatePaymentEvent cancelledCreatePaymentEvent = new CancelledCreatePaymentEvent();
        BeanUtils.copyProperties(cancelCreatePaymentCommand, cancelledCreatePaymentEvent);

        AggregateLifecycle.apply(cancelledCreatePaymentEvent);
    }

    @EventSourcingHandler
    private void on(CancelledCreatePaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing CancelledCreatePaymentEvent for Order Id: {} and Payment Id: {}",
                event.getOrderId(), event.getPaymentId());

        this.paymentStatus = event.getPaymentStatus();
    }

    @CommandHandler
    private void handle(UpdatePaymentCommand cmd) {
        log.info("[@CommandHandler] Executing UpdatePaymentCommand for Order Id: {} and Payment Id: {}",
                cmd.getOrderId(), cmd.getPaymentId());
        UpdatedPaymentEvent updatedPaymentEvent = new UpdatedPaymentEvent();
        BeanUtils.copyProperties(cmd, updatedPaymentEvent);

        AggregateLifecycle.apply(updatedPaymentEvent);
    }

    @EventSourcingHandler
    private void on(UpdatedPaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing UpdatedPaymentEvent for Order Id: {} and Payment Id: {}",
                event.getOrderId(), event.getPaymentId());

        //-- 수정 또는 삭제 실패 시 이전 정보로 rollback시에만 사용되므로 바로 이전 정보만 담고 있으면 됨
        this.aggregateHistory.clear();
        this.aggregateHistory.add(cloneAggregate(this));

        this.totalPaymentAmt = event.getTotalPaymentAmt();
        this.paymentStatus = event.getPaymentStatus();
        this.paymentDetails.clear();
        for(PaymentDetailDTO item:event.getPaymentDetails()) {
            this.paymentDetails.add(new PaymentDetail(
                    (new PaymentDetailIdentity(item.getPaymentId(), item.getPaymentKind())),
                    item.getPaymentAmt()));
        }
    }

    @CommandHandler
    private void handle(CancelUpdatePaymentCommand cancelUpdatePaymentCommand) {
        log.info("[@CommandHandler] Executing CancelUpdatePaymentCommand for Order Id: {} and Payment Id: {}",
                cancelUpdatePaymentCommand.getOrderId(), cancelUpdatePaymentCommand.getPaymentId());
        CancelledUpdatePaymentEvent cancelledUpdatePaymentEvent = new CancelledUpdatePaymentEvent();
        BeanUtils.copyProperties(cancelUpdatePaymentCommand, cancelledUpdatePaymentEvent);

        AggregateLifecycle.apply(cancelledUpdatePaymentEvent);

        //-- send UpdateOrderCommand to compensate
        if(this.aggregateHistory.isEmpty()) return;
        PaymentDTO payment = this.aggregateHistory.get(this.aggregateHistory.size()-1);
        UpdatePaymentCommand cmd = UpdatePaymentCommand.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .totalPaymentAmt(payment.getTotalPaymentAmt())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDetails(payment.getPaymentDetails())
                .isCompensation(cancelUpdatePaymentCommand.isCompensation())
                .build();
        commandGateway.send(cmd);
    }

    @EventSourcingHandler
    private void on(CancelledUpdatePaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing <CancelledUpdatePaymentEvent> for Order Id: {}", event.getOrderId());

    }

    @CommandHandler
    private void handle(DeletePaymentCommand deletePaymentCommand) {
        log.info("[@CommandHandler] Executing DeletedPaymentEvent for Order Id: {} and Payment Id: {}",
                deletePaymentCommand.getOrderId(), deletePaymentCommand.getPaymentId());

        AggregateLifecycle.apply(new DeletedPaymentEvent(deletePaymentCommand.getPaymentId(), deletePaymentCommand.getOrderId()));
    }
    @EventSourcingHandler
    private void on(DeletedPaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing DeletedPaymentEvent for Order Id: {} and Payment Id: {}",
                event.getOrderId(), event.getPaymentId());
        this.paymentStatus = PaymentStatusEnum.ORDER_CANCLLED.value();
    }

    @CommandHandler
    private void handle(CancelDeletePaymentCommand cancelDeletePaymentCommand) {
        log.info("[@EventSourcingHandler] Executing CancelDeletePaymentCommand for Order Id: {} and Payment Id: {}",
                cancelDeletePaymentCommand.getOrderId(), cancelDeletePaymentCommand.getPaymentId());
        AggregateLifecycle.apply(new CancelledDeletePaymentEvent(
                cancelDeletePaymentCommand.getPaymentId(),
                cancelDeletePaymentCommand.getOrderId(), true));

        //-- send CreateOrderCommand to compensate
        if(this.aggregateHistory.isEmpty()) return;
        PaymentDTO payment = this.aggregateHistory.get(this.aggregateHistory.size()-1);
        CreatePaymentCommand cmd = CreatePaymentCommand.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .totalPaymentAmt(payment.getTotalPaymentAmt())
                .paymentDetails(payment.getPaymentDetails())
                .isCompensation(true)
                .build();
        commandGateway.send(cmd);
    }
    @EventSourcingHandler
    private void on(CancelledDeletePaymentEvent event) {
        log.info("[@EventSourcingHandler] Executing CancelledDeletePaymentEvent for Order Id: {} and Payment Id: {}",
                event.getOrderId(), event.getPaymentId());

    }

    private PaymentDTO cloneAggregate(PaymentAggregate paymentAggregate) {
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setOrderId(paymentAggregate.orderId);
        paymentDTO.setPaymentId(paymentAggregate.paymentId);
        paymentDTO.setTotalPaymentAmt(paymentAggregate.totalPaymentAmt);
        paymentDTO.setPaymentStatus(paymentAggregate.paymentStatus);
        paymentDTO.setPaymentDetails(paymentAggregate.paymentDetails.stream()
                .map(o->new PaymentDetailDTO(paymentAggregate.orderId, paymentAggregate.paymentId,
                        o.getPaymentDetailIdentity().getPaymentKind(), o.getPaymentAmt()))
                .collect(Collectors.toList()));

        return paymentDTO;
    }
}
