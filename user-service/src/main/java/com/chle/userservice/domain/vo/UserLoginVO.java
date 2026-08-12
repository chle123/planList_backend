package com.chle.userservice.domain.vo;

import lombok.Data;

@Data
public class UserLoginVO {
    private Long userId;
    private String username;
    private String token;
}
