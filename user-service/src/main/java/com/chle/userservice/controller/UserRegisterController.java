package com.chle.userservice.controller;

import com.chle.common.result.Result;
import com.chle.userservice.domain.dto.RegisterFormDTO;
import com.chle.userservice.domain.vo.UserLoginVO;
import com.chle.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RestController
@RequiredArgsConstructor
public class UserRegisterController {
    private final UserService userService;
    @PostMapping("register")
    public Result<UserLoginVO> register(@RequestBody RegisterFormDTO registerFormDTO) {
        UserLoginVO register = userService.register(registerFormDTO);
        return Result.ok(register);
    }
}
