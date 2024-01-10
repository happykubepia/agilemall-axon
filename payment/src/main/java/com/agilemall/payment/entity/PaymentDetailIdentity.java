package com.agilemall.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Embeddable
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentDetailIdentity implements Serializable {
    @Serial
    private static final long serialVersionUID = 3759455937854609084L;

    @Column(name="payment_id", nullable = false, length = 15)
    private String paymentId;

    @Column(name = "payment_kind", nullable = false, length = 2)
    private String paymentKind;

}
