package com.agilemall.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResultVO<T> {
    private boolean returnCode;
    private String returnMessage;
    private T result;
}
