package com.chle.userservice.domain.dto;

import lombok.Data;

@Data
public class LoginFormDTO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
}
