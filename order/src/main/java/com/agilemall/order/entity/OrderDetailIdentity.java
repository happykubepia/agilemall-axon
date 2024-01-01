package com.agilemall.order.entity;

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
public class OrderDetailIdentity implements Serializable {
    @Serial
    private static final long serialVersionUID = -3422409301899168805L;
    /*
    - 참고: serialVersionUID: https://velog.io/@hellonewtry/%EC%9E%90%EB%B0%94-%EC%A7%81%EB%A0%AC%ED%99%94%EB%9E%80-serialVersionUID-%EB%9E%80
    - 부여방법(IntelliJ): serialVersionUID에 마우스 커서를 올리고 '전구'아이콘 메뉴에서 'randomly change serialVersionUID initializer'를 선택
     */

    @Column(name="order_id", nullable=false, length=15)
    private String orderId;

    @Column(name="order_seq", nullable = false)
    private int orderSeq;
}
