package com.agilemall.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeliveryDTO {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
