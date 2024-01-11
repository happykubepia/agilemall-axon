package com.agilemall.common.events.delete;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeletedDeliveryEvent {
    private String deliveryId;
    private String orderId;
}
