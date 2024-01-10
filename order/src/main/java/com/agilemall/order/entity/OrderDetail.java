package com.agilemall.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.EntityId;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="order_detail")
public class OrderDetail implements Serializable {
    @Serial
    private static final long serialVersionUID = 3618758486649780850L;

    @EmbeddedId
    @EntityId
    private OrderDetailIdentity orderDetailIdentity;

    @Column(name="qty", nullable = false)
    private int qty;

    @Column(name="order_amt", nullable = false)
    private int orderAmt;
}
