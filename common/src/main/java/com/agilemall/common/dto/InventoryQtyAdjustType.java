package com.agilemall.common.dto;

public enum InventoryQtyAdjustType {
    INCREASE("+"),
    DECREASE("-");

    private String value;

    InventoryQtyAdjustType(String value) {
        this.value = value;
    }
    public String value() {
        return value;
    }
}
