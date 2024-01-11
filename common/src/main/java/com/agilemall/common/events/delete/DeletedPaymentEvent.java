package com.agilemall.common.events.delete;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeletedPaymentEvent {
    private String paymentId;
    private String orderId;
}
