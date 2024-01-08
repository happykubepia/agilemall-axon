package com.agilemall.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderDetailDTO {
    private String orderId;
    private String productId;
    private int orderSeq;
    private int qty;
    private int orderAmt;
}
