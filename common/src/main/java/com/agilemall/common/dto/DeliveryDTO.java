package com.agilemall.common.dto;

import lombok.Data;

@Data
public class DeliveryDTO {
    private String deliveryId;
    private String orderId;
    private String deliveryStatus;
}
