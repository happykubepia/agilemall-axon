package com.agilemall.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
@Entity
@Table(name="payment_detail")
public class PaymentDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 1743029137337987926L;

    @EmbeddedId
    private PaymentDetailIdentity paymentDetailIdentity;

    @Column(name = "payment_amt", nullable = false)
    private int paymentAmt;
}
