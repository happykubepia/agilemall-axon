package com.agilemall.payment.events;

import com.agilemall.common.dto.PaymentDetailDTO;
import com.agilemall.common.dto.PaymentStatus;
import com.agilemall.common.events.PaymentCancelledEvent;
import com.agilemall.common.events.PaymentProcessedEvent;
import com.agilemall.payment.entity.Payment;
import com.agilemall.payment.entity.PaymentDetail;
import com.agilemall.payment.entity.PaymentDetailIdentity;
import com.agilemall.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class PaymentEventHandler {
    @Autowired
    private PaymentRepository paymentRepository;

    @EventHandler
    public void on(PaymentProcessedEvent event) {
        log.info("[@EventHandler] Executing on..");
        log.info(event.toString());

        List<PaymentDetail> newPaymentDetails = new ArrayList<>();

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

            newPaymentDetails.add(newPaymentDetail);
        }
        payment.setPaymentDetails(newPaymentDetails);

        paymentRepository.save(payment);
    }

    @EventHandler
    public void on(PaymentCancelledEvent event) {
        Payment payment = paymentRepository.findById(event.getPaymentId()).get();
        payment.setPaymentStatus(PaymentStatus.CANCELED.value());

        paymentRepository.save(payment);
    }
}
