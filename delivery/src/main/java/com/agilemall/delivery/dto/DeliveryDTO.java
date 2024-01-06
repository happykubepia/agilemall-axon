package com.agilemall.delivery.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DeliveryDTO {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
