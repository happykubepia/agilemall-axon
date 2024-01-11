package com.agilemall.common.events.delete;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FailedDeleteReportEvent {
    private String reportId;
    private String orderId;
}
