package com.agilemall.common.events.update;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedUpdatePaymentEvent {
    private String paymentId;
    private String orderId;
}
