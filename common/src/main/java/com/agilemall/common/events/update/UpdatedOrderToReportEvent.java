package com.agilemall.common.events.update;

import com.agilemall.common.dto.OrderDetailDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class UpdatedOrderToReportEvent {
    private String orderId;
    private LocalDateTime orderDatetime;
    private int totalOrderAmt;
    private List<OrderDetailDTO> orderDetails;
    private String orderStatus;
}
