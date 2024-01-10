package com.agilemall.common.events.create;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedCreatePaymentEvent {
    private String paymentId;
    private String orderId;
}
