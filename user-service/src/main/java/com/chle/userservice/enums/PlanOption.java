package com.chle.userservice.enums;

public enum PlanOption {
    DELETE(1, "删除"),
    UPDATE(2, "更新"),
    ADD(3, "添加");
    private int value;
    private String message;
    PlanOption(int value, String message) {
        this.value = value;
        this.message = message;
    }
    public static PlanOption of(int value) {
        for (PlanOption planOption : PlanOption.values()) {
            if (planOption.value == value) {
                return planOption;
            }
        }
        throw new IllegalArgumentException("无效的PlanOption值: " + value);
    }
}
