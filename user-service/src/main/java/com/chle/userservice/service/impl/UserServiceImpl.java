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
import org.springframework.util.Assert;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{
    private final JwtTool jwtTool;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Override
    public UserLoginVO login(LoginFormDTO loginFromDto) {
        //获取数据
        String username = loginFromDto.getUsername();
        String password = loginFromDto.getPassword();

        //校验数据
        User user = this.getOne(new LambdaQueryWrapper<>(User.class).eq(User::getUsername, username));
        Assert.notNull(user, "用户名错误");

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("用户名或密码错误");
        }
        String token = jwtTool.createToken(user.getId(), jwtProperties.getTokenTTL());
        // 封装VO返回
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setToken(token);
        return vo;
    }

    @Override
    public UserLoginVO register(RegisterFormDTO registerFormDTO) {
        String username = registerFormDTO.getUsername();
        String password = registerFormDTO.getPassword();
        //判断是否存在
        long count = this.count(new LambdaQueryWrapper<>(User.class).eq(User::getUsername, username));
        if (count > 0) {
            throw new BadRequestException("用户名已存在"); // 或返回特定错误码
        }
        //保存用户
        User user = new User()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(password));

        boolean saved = this.save(user);
        if (!saved) {
            throw new BadRequestException("注册失败，请稍后重试");
        }
        //生成token
        Long userId = user.getId();
        String token = jwtTool.createToken(userId, Duration.ofMinutes(30));
        //返回vo对象
        UserLoginVO vo = new UserLoginVO();
        vo.setUserId(userId);
        vo.setUsername(username);
        vo.setToken(token);

        return vo;
    }

}




