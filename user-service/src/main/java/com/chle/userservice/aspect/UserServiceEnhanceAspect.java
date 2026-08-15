package com.chle.userservice.aspect;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chle.common.constant.AppConstants;          // 新增导入
import com.chle.common.exception.BadRequestException;
import com.chle.userservice.config.JwtProperties;
import com.chle.userservice.domain.User;
import com.chle.userservice.domain.dto.LoginFormDTO;
import com.chle.userservice.domain.dto.RegisterFormDTO;
import com.chle.userservice.domain.vo.UserLoginVO;
import com.chle.userservice.mapper.UserMapper;
import com.chle.userservice.utils.JwtTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceEnhanceAspect {

    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtTool jwtTool;
    private final JwtProperties jwtProperties;
    private final UserMapper userMapper;

    @Around("execution(* com.chle.userservice.service.impl.UserServiceImpl.login(..))")
    public Object enhanceLogin(ProceedingJoinPoint pjp) throws Throwable {
        LoginFormDTO dto = (LoginFormDTO) pjp.getArgs()[0];
        String username = dto.getUsername();
        String password = dto.getPassword();

        // 使用常量：AppConstants.User.LOCK_KEY_PREFIX
        String lockKey = AppConstants.User.LOCK_KEY_PREFIX + username;
        String failCountStr = redisTemplate.opsForValue().get(lockKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= AppConstants.User.MAX_FAIL_COUNT) {
            throw new BadRequestException("账号因多次登录失败已被锁定，请稍后重试");
        }

        String cacheKey = AppConstants.User.CACHE_KEY_PREFIX + username;
        String userJson = redisTemplate.opsForValue().get(cacheKey);
        User cachedUser = null;
        if (userJson != null) {
            cachedUser = JSONUtil.toBean(userJson, User.class);
        }

        if (cachedUser != null) {
            log.info("缓存命中，绕过数据库查询，username: {}", username);
            if (passwordEncoder.matches(password, cachedUser.getPassword())) {
                redisTemplate.delete(lockKey);
                String token = jwtTool.createToken(cachedUser.getId(), jwtProperties.getTokenTTL());
                return UserLoginVO.builder()
                        .userId(cachedUser.getId())
                        .username(cachedUser.getUsername())
                        .token(token)
                        .build();
            } else {
                incrementFailCount(lockKey);
                throw new BadRequestException("用户名或密码错误");
            }
        }

        try {
            Object result = pjp.proceed();
            User dbUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
            if (dbUser != null) {
                // 使用常量：AppConstants.User.CACHE_TTL_MINUTES
                redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(dbUser),
                        AppConstants.User.CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            }
            redisTemplate.delete(lockKey);
            return result;
        } catch (BadRequestException e) {
            incrementFailCount(lockKey);
            throw e;
        }
    }

    @AfterReturning("execution(* com.chle.userservice.service.impl.UserServiceImpl.register(..))")
    public void enhanceRegister(JoinPoint jp) {
        RegisterFormDTO dto = (RegisterFormDTO) jp.getArgs()[0];
        String username = dto.getUsername();
        String cacheKey = AppConstants.User.CACHE_KEY_PREFIX + username;
        redisTemplate.delete(cacheKey);
        log.info("用户注册成功，已清除缓存，username: {}", username);
    }

    private void incrementFailCount(String lockKey) {
        Long newCount = redisTemplate.opsForValue().increment(lockKey);
        if (newCount != null && newCount == 1) {
            // 使用常量：AppConstants.User.LOCK_DURATION_MINUTES
            redisTemplate.expire(lockKey, AppConstants.User.LOCK_DURATION_MINUTES, TimeUnit.MINUTES);
        }
    }
}