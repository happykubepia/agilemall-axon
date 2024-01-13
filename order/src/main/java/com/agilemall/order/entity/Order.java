package com.agilemall.order.entity;
/*
- 목적: Entity를 정의하며 JPA(Java Persistence API)로 CRUD할 Table구조를 정의
*/
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name="orders")
public final class Order implements Serializable {
    /*
    Serialize(DB에 저장 시 binary로 변환)와 Deserialize(DB에 저장된 binary 데이터를 원래 값으로 변환)시 사용할 UID
    'serialVersionUID에 마우스를 올려놓고 전구 아이콘 메뉴에서 <Randomly change 'serialVersionUID' initializer> 선택
    */
    @Serial
    private static final long serialVersionUID = 6965475979801800704L;

    @Id     //Primary key 필드를 나타냄
    @Column(name="order_id", nullable = false, length = 15)
    private String orderId;

    @Column(name="user_id", nullable = false, length = 30)
    private String userId;

    @Column(name = "order_datetime",nullable = false, columnDefinition = "TIMESTAMP")
    private LocalDateTime orderDatetime;

    @Column(name="order_status", nullable = false, length = 2)
    private String orderStatus;

    @Column(name="total_order_amt", nullable = false)
    private int totalOrderAmt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true) //orphanRemoval: Order 삭제 시 연결된 OrderDetail 데이터도 삭제
    @JoinColumn(name="order_id", updatable = false)
    private List<OrderDetail> orderDetails;
}
