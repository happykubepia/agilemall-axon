package com.agilemall.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data   //@Getter @Setter @RequiredArgsConstructor @ToString @EqualsAndHashCode 포함
@AllArgsConstructor //모든 프라퍼티를 인자로 가지는 생성자 자동 생성
@NoArgsConstructor  //인자가 없는 생성자 자동 생성
public class OrderReqCreateDTO {
    private String userId;
    private List<OrderReqDetailDTO> orderReqDetails;
    private List<PaymentReqDetailDTO> paymentReqDetails;
}
