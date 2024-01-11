package com.agilemall.common.events.delete;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancelledDeletePaymentEvent {
    private String paymentId;
    private String orderId;
    private boolean isCompensation;
}
