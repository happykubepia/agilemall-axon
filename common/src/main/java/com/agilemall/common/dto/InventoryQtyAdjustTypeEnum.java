package com.agilemall.common.dto;

public enum InventoryQtyAdjustTypeEnum {
    INCREASE("+"),
    DECREASE("-");

    private String value;

    InventoryQtyAdjustTypeEnum(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
