package com.agilemall.payment.events;

import com.agilemall.common.command.CancelOrderCommand;
import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.dto.ServiceNameEnum;
import com.agilemall.common.events.PaymentCancelledEvent;
import com.agilemall.common.events.PaymentCreatedEvent;
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
    private void on(PaymentCreatedEvent event) {
        log.info("[@EventHandler] Handle <PaymentProcessedEvent> for Payment Id: {}", event.getPaymentId());
        log.info(event.toString());

        List<PaymentDetail> newPaymentDetails = new ArrayList<>();

        try {
            Payment payment = new Payment();
            payment.setPaymentId(event.getPaymentId());
            payment.setOrderId(event.getOrderId());
            payment.setTotalPaymentAmt(event.getTotalPaymentAmt());
            payment.setPaymentStatus(PaymentStatusEnum.COMPLETED.value());

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
            cancelOrder(aggregateIdMap);
            //------------------------------
        }
    }

    @EventHandler
    private void on(PaymentCancelledEvent event) {
        log.info("[@EventHandler] Handle <PaymentCancelledEvent> for Payment Id: {}", event.getPaymentId());
        Payment payment = paymentRepository.findById(event.getPaymentId()).get();
        payment.setPaymentStatus(PaymentStatusEnum.CANCELED.value());

        paymentRepository.save(payment);
    }

    private void cancelOrder(HashMap<String, String> aggregateIdMap) {
        log.info("[PaymentEventHandler] cancelOrder for Order Id: {}", aggregateIdMap.get(ServiceNameEnum.ORDER.value()));

        try {
            CancelOrderCommand cancelOrderCommand = CancelOrderCommand.builder()
                    .orderId(aggregateIdMap.get(ServiceNameEnum.ORDER.value())).build();
            commandGateway.sendAndWait(cancelOrderCommand);
        } catch(Exception e) {
            log.error("Error is occurred during <cancelOrderCommand>: {}", e.getMessage());
        }
    }

}
