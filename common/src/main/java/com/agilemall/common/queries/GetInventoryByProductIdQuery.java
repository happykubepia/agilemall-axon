package com.agilemall.common.queries;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GetInventoryByProductIdQuery {
    private String productId;
}
