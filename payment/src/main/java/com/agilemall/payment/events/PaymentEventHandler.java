package com.agilemall.payment.events;

import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatus;
import com.agilemall.common.dto.ServiceName;
import com.agilemall.common.events.PaymentCancelledEvent;
import com.agilemall.common.events.PaymentProcessedEvent;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.entity.PaymentDetail;
import com.agilemall.payment.entity.PaymentDetailIdentity;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
//@EnableRetry
public class PaymentEventHandler {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private transient CommandGateway commandGateway;

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("[@EventHandler] Executing handle <PaymentProcessedEvent>");
        log.info(event.toString());

        List<PaymentDetail> newPaymentDetails = new ArrayList<>();

        try {
            Payment payment = new Payment();
            payment.setPaymentId(event.getPaymentId());
            payment.setOrderId(event.getOrderId());
            payment.setTotalPaymentAmt(event.getTotalPaymentAmt());
            payment.setPaymentStatus(PaymentStatus.COMPLETED.value());

            for(PaymentDetailDTO paymentDetail:event.getPaymentDetails()) {
                PaymentDetailIdentity paymentDetailIdentity = new PaymentDetailIdentity(
                        paymentDetail.getPaymentId(), paymentDetail.getPaymentGbcd()
                );
                PaymentDetail newPaymentDetail = new PaymentDetail();
                newPaymentDetail.setPaymentDetailIdentity(paymentDetailIdentity);
                newPaymentDetail.setPaymentAmt(paymentDetail.getPaymentAmt());
                newPaymentDetails.add(newPaymentDetail);
            }
            payment.setPaymentDetails(newPaymentDetails);

            paymentRepository.save(payment);
        } catch(Exception e) {
            log.error("Error is occurred during handle <PaymentProcessedEvent>: {}", e.getMessage());
            //-- request compensating transactions
            HashMap<String, String> aggregateIdMap = event.getAggregateIdMap();
            // compensate Order
            compensateOrder(aggregateIdMap);
            //------------------------------
        }
    }

    @EventHandler
    public void on(PaymentCancelledEvent event) {
        Payment payment = paymentRepository.findById(event.getPaymentId()).get();
        payment.setPaymentStatus(PaymentStatus.CANCELED.value());

        paymentRepository.save(payment);
    }

    private void compensateOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[PaymentEventHandler] compensateOrder for Order Id: {}", aggregateIdMap.get(ServiceName.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = new CancelOrderCommand(aggregateIdMap.get(ServiceName.ORDER.value()));
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }
    }
/*
    @EventHandler
    @Retryable(
            maxAttempts = Constants.RETRYABLE_MAXATTEMPTS,
            retryFor = { IOException.class, TimeoutException.class, RuntimeException.class},
            backoff = @Backoff(delay = Constants.RETRYABLE_DELAY)
    )
    public void on(ReportUpdateEvent event) {
        log.info("[@EventHandler] Handle ReportUpdateEvent");

    }

 */
}
