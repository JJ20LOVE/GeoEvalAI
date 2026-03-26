package com.geollm.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.geollm.dto.user.ChangePassParam;
import com.geollm.dto.user.LoginParam;
import com.geollm.dto.user.SignUpParam;
import com.geollm.entity.User;
import com.geollm.mapper.UserMapper;
import com.geollm.service.UserService;
import com.geollm.utils.JwtUtil;
import com.geollm.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(UserMapper userMapper, @Value("${app.jwtKey}") String jwtKey) {
        this.userMapper = userMapper;
        this.jwtUtil = new JwtUtil(jwtKey);
    }

    @Override
    public int signUp(SignUpParam p) {
        log.debug("UserService.signUp username={} email={}", p.getName(), p.getEmail());
        if (!p.getPassword().equals(p.getRe_password())) {
            log.info("UserService.signUp rejected: password mismatch username={}", p.getName());
            return 300;
        }
        long cnt = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, p.getName()));
        if (cnt > 0) {
            log.info("UserService.signUp rejected: user exists username={}", p.getName());
            return 301;
        }

        User u = new User();
        u.setUsername(p.getName());
        u.setEmail(p.getEmail());
        u.setPassword(PasswordUtil.scryptBase64(p.getPassword()));
        userMapper.insert(u);
        log.info("UserService.signUp ok user_id={}", u.getUserId());
        return 200;
    }

    @Override
    public String login(LoginParam p) {
        log.debug("UserService.login username={}", p.getUsername());
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, p.getUsername()));
        if (u == null) {
            log.info("UserService.login failed: user not found username={}", p.getUsername());
            return null;
        }
        if (!PasswordUtil.scryptBase64(p.getPassword()).equals(u.getPassword())) {
            log.info("UserService.login failed: bad credentials username={}", p.getUsername());
            return null;
        }
        String token = jwtUtil.generateToken(Map.of("username", u.getUsername(), "user_id", u.getUserId()),
                7L * 24 * 3600 * 1000);
        log.info("UserService.login ok user_id={}", u.getUserId());
        return token;
    }

    @Override
    public int changePassword(ChangePassParam p) {
        log.debug("UserService.changePassword username={}", p.getUsername());
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, p.getUsername()));
        if (u == null) {
            log.info("UserService.changePassword failed: user not found username={}", p.getUsername());
            return 310;
        }
        if (!PasswordUtil.scryptBase64(p.getOld_pass()).equals(u.getPassword())) {
            log.info("UserService.changePassword failed: bad old password username={}", p.getUsername());
            return 311;
        }
        u.setPassword(PasswordUtil.scryptBase64(p.getNew_pass()));
        userMapper.updateById(u);
        log.info("UserService.changePassword ok user_id={}", u.getUserId());
        return 200;
    }
}

