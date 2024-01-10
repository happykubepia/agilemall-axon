package com.agilemall.common.events.create;

import lombok.Data;

@Data
public class CancelledCreatePaymentEvent {
    private String paymentId;
    private String orderId;
    private String paymentStatus;
}
