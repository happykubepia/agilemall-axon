package com.agilemall.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DeliveryDTO {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
