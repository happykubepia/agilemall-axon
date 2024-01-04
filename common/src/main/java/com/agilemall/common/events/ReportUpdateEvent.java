package com.agilemall.common.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportUpdateEvent {
    private String orderId;
    private String orderStatus;
}
