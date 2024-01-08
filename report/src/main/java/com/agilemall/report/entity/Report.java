package com.agilemall.report.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "report")
public class Report implements Serializable {
    @Serial
    private static final long serialVersionUID = -2315607591930989462L;

    @Id
    @Column(name = "report_id", nullable = false, length = 15)
    private String reportId;

    @Column(name = "order_id", nullable = false, length = 15)
    private String orderId;

    @Column(name = "user_id", nullable = false, length = 30)
    private String userId;

    @Column(name = "order_datetime", nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDatetime;

    @Column(name = "total_order_amt", nullable = false)
    private int totalOrderAmt;

    @Column(name = "order_status", nullable = false, length = 2)
    private String orderStatus;

    @Column(name = "order_details", nullable = true, length = 3000)
    private String orderDetails;

    @Column(name = "payment_id", nullable = false, length = 15)
    private String paymentId;

    @Column(name = "total_payment_amt", nullable = false)
    private int totalPaymentAmt;

    @Column(name = "payment_status", nullable = false, length = 2)
    private String paymentStatus;

    @Column(name = "payment_details", nullable = true, length = 3000)
    private String paymentDetails;

    @Column(name = "delivery_id", nullable = false, length = 15)
    private String deliveryId;

    @Column(name = "delivery_status", nullable = false, length = 2)
    private String deliveryStatus;

}
