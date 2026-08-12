package com.chle.userservice.controller;


import com.chle.common.result.Result;
import com.chle.userservice.domain.dto.LoginFormDTO;
import com.chle.userservice.domain.vo.UserLoginVO;
import com.chle.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="用户登录接口")
@Controller
@RestController
@RequiredArgsConstructor
public class UserLoginController {

    private final UserService userService;
    @Operation(summary = "用户登录请求")
    @Schema(description = "用户账号密码")
    @PostMapping("login")
    public Result<UserLoginVO> login(@RequestBody @Validated LoginFormDTO loginFormDTO) {
        UserLoginVO vo = userService.login(loginFormDTO);
        return Result.ok(vo);
    }

}
