package com.agilemall.common.quries;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetOrderDetailsByOrderIdQuery {
    private String orderId;
}
