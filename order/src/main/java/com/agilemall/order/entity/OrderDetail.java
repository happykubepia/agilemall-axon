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
    @Serial     //Entity 'Order' 참조
    private static final long serialVersionUID = 3618758486649780850L;

    @EmbeddedId //Primay key가 복수 필드인 경우 @Id 대신 @EmbeddedId 어노테이션 사용
    @EntityId   //OrderAggregate의 @AggregateMember로 지정된 필드 중 복수값 필드의 Unique key임을 나타냄
    private OrderDetailIdentity orderDetailIdentity;

    @Column(name="qty", nullable = false)
    private int qty;

    @Column(name="order_amt", nullable = false)
    private int orderAmt;
}
