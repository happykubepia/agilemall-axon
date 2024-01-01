package com.agilemall.order.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Table(name="orders")
public final class Order implements Serializable {
    @Serial
    private static final long serialVersionUID = 6965475979801800704L;

    @Id
    @Column(name="order_id", nullable = false, length = 15)
    private String orderId;

    @Column(name="user_id", nullable = false, length = 30)
    private String userId;

    @Column(name="order_status", nullable = false, length = 2)
    private String orderStatus;

    @Column(name="total_order_amt", nullable = false)
    private int totalOrderAmt;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name="order_id", updatable = false)
    private List<OrderDetail> orderDetails;
}
