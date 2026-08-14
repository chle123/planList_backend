package com.chle.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chle.userservice.utils.JwtTool;
import com.chle.userservice.config.JwtProperties;
import com.chle.userservice.domain.User;
import com.chle.userservice.domain.dto.LoginFormDTO;
import com.chle.userservice.domain.dto.RegisterFormDTO;
import com.chle.userservice.domain.vo.UserLoginVO;
import com.chle.userservice.service.UserService;
import com.chle.userservice.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import com.chle.common.exception.BadRequestException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final JwtTool jwtTool;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO login(LoginFormDTO loginFromDto) {
        String username = loginFromDto.getUsername();
        String password = loginFromDto.getPassword();

        User user = this.getOne(new LambdaQueryWrapper<>(User.class).eq(User::getUsername, username));
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        return UserLoginVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .token(token)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLoginVO register(RegisterFormDTO registerFormDTO) {
        String username = registerFormDTO.getUsername();
        String password = registerFormDTO.getPassword();

        long count = this.count(new LambdaQueryWrapper<>(User.class).eq(User::getUsername, username));
        if (count > 0) {
            throw new BadRequestException("用户名已存在");
        }

        User user = new User()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(password));
        this.save(user);

        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        return UserLoginVO.builder()
                .userId(user.getId())
                .username(username)
                .token(token)
                .build();
    }
}




