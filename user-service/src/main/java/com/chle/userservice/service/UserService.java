package com.chle.userservice.service;

import com.chle.userservice.domain.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chle.userservice.domain.dto.LoginFormDTO;
import com.chle.userservice.domain.dto.RegisterFormDTO;
import com.chle.userservice.domain.vo.UserLoginVO;

public interface UserService extends IService<User> {
    UserLoginVO login(LoginFormDTO loginFromDto);
    UserLoginVO register(RegisterFormDTO registerFormDTO);

}
