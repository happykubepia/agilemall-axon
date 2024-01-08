package com.agilemall.common.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResultVO<T> {
    private boolean returnCode;
    private String returnMessage;
    private T result;
}
