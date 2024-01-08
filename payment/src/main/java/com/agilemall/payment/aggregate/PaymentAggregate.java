package com.agilemall.payment.aggregate;

import com.agilemall.common.command.CancelPaymentCommand;
import com.agilemall.common.command.CreatePaymentCommand;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.events.PaymentCancelledEvent;
import com.agilemall.common.events.PaymentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Aggregate
@Slf4j
public class PaymentAggregate {
    @AggregateIdentifier
    private String paymentId;

    private String orderId;
    private int totalPaymentAmt;
    private String paymentStatus;
    private List<PaymentDetailDTO> paymentDetails;

    public PaymentAggregate() {

    }

    @CommandHandler
    public PaymentAggregate(CreatePaymentCommand createPaymentCommand) {
        log.info("[@CommandHandler] Executing PaymentAggregate..");
        log.info(createPaymentCommand.toString());

        PaymentCreatedEvent paymentCreatedEvent = new PaymentCreatedEvent();
        BeanUtils.copyProperties(createPaymentCommand, paymentCreatedEvent);

        AggregateLifecycle.apply(paymentCreatedEvent);
    }

    @EventSourcingHandler
    public void on(PaymentCreatedEvent event) {
        log.info("[@EventSourcingHandler] Executing on ..");
        this.paymentId = event.getPaymentId();
        this.orderId = event.getOrderId();
        this.totalPaymentAmt = event.getTotalPaymentAmt();
        this.paymentDetails = event.getPaymentDetails();
    }

    @CommandHandler
    public void handle(CancelPaymentCommand cancelPaymentCommand) {
        log.info("[@CommandHandler] Executing CancelPaymentCommand for Order Id: {} and Payment Id: {}",
                cancelPaymentCommand.getOrderId(), cancelPaymentCommand.getPaymentId());

        PaymentCancelledEvent paymentCancelledEvent = new PaymentCancelledEvent();
        BeanUtils.copyProperties(cancelPaymentCommand, paymentCancelledEvent);

        AggregateLifecycle.apply(paymentCancelledEvent);
    }

    @EventSourcingHandler
    public void on(PaymentCancelledEvent event) {
        log.info("[@EventSourcingHandler] Executing PaymentCancelledEvent for Order Id: {} and Payment Id: {}",
                event.getOrderId(), event.getPaymentId());

        this.paymentStatus = event.getPaymentStatus();
    }

}
