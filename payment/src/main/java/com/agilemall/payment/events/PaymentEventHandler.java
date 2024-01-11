package com.agilemall.payment.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatusEnum;
import com.agilemall.common.events.create.CancelledCreatePaymentEvent;
import com.agilemall.common.events.create.CreatedPaymentEvent;
import com.agilemall.common.events.create.FailedCreatePaymentEvent;
import com.agilemall.common.events.delete.DeletedPaymentEvent;
import com.agilemall.common.events.delete.FailedDeletePaymentEvent;
import com.agilemall.common.events.update.FailedUpdatePaymentEvent;
import com.agilemall.common.events.update.UpdatedPaymentEvent;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.entity.PaymentDetail;
import com.agilemall.payment.entity.PaymentDetailIdentity;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.AllowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ProcessingGroup("payment")
@AllowReplay
public class PaymentEventHandler {
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private transient EventGateway eventGateway;

    @EventHandler
    private void on(CreatedPaymentEvent event) {
        log.info("[@EventHandler] Handle <CreatedPaymentEvent> for Payment Id: {}", event.getPaymentId());
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
                        paymentDetail.getPaymentId(), paymentDetail.getPaymentKind()
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
            eventGateway.publish(new FailedCreatePaymentEvent(event.getPaymentId(), event.getOrderId()));
        }
    }

    @EventHandler
    private void on(CancelledCreatePaymentEvent event) {
        log.info("[@EventHandler] Handle <CancelledCreatePaymentEvent> for Payment Id: {}", event.getPaymentId());
        Payment payment = getEntity(event.getPaymentId());
        if(payment != null) {
            paymentRepository.delete(payment);
        }
    }

    @EventHandler
    private void on(UpdatedPaymentEvent event) {
        log.info("[@EventHandler] Handle <UpdatedPaymentEvent> for Payment Id: {}", event.getPaymentId());
        Payment payment = getEntity(event.getPaymentId());
        if(payment == null) {
            eventGateway.publish(new FailedUpdatePaymentEvent(event.getPaymentId(), event.getOrderId()));
            return;
        }

        try {
            payment.setTotalPaymentAmt(event.getTotalPaymentAmt());
            payment.setPaymentStatus(PaymentStatusEnum.COMPLETED.value());
            payment.getPaymentDetails().clear();
            for(PaymentDetailDTO item:event.getPaymentDetails()) {
                payment.getPaymentDetails().add(new PaymentDetail(
                        (new PaymentDetailIdentity(item.getPaymentId(), item.getPaymentKind())),
                        item.getPaymentAmt()));
            }

            paymentRepository.save(payment);
        } catch(Exception e) {
            log.error(e.getMessage());
            eventGateway.publish(new FailedUpdatePaymentEvent(event.getPaymentId(), event.getOrderId()));
        }
    }

    @EventHandler
    private void on(DeletedPaymentEvent event) {
        log.info("[@EventHandler] Handle <DeletedPaymentEvent> for Payment Id: {}", event.getPaymentId());
        Payment payment = getEntity(event.getPaymentId());
        if(payment == null) {
            eventGateway.publish(new FailedDeletePaymentEvent(event.getPaymentId(), event.getOrderId()));
            return;
        }
        if(payment != null) {
            try {
                paymentRepository.delete(payment);
            } catch(Exception e) {
                log.error(e.getMessage());
                eventGateway.publish(new FailedDeletePaymentEvent(event.getPaymentId(), event.getOrderId()));
            }
        }
    }

    private Payment getEntity(String paymentId) {
        Optional<Payment> optPayment = paymentRepository.findById(paymentId);
        if(optPayment.isPresent()) {
            return optPayment.get();
        } else {
            log.info("Can't find Payment info for Payment Id: {}", paymentId);
            return null;
        }
    }

    @ResetHandler
    private void replayAll() {
        log.info("[PaymentEventHandler] Executing replayAll");
        paymentRepository.deleteAll();
    }
}
