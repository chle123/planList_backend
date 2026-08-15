package com.chle.userservice.enums;

import lombok.Getter;

@Getter
public enum PlanStatus {
    NOT_COMPLETE(0,"未完成"),
    COMPLETE(1,"完成"),
    TIME_OUT(2,"过期 "),
    IN_PROGRESS(3,"进行中");

    private int value;
    private String message;
    PlanStatus(int value,String message){
        this.value = value;
        this.message = message;
    }
    public static PlanStatus of(int value){
        for (PlanStatus planStatus : PlanStatus.values()) {
            if (planStatus.value == value) {
                return planStatus;
            }
        }
        throw new IllegalArgumentException("无效的PlanStatus值: " + value);
    }
}
