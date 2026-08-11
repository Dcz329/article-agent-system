package com.deng.article.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.deng.article.common.BizException;
import com.deng.article.config.JwtUtil;
import com.deng.article.dto.LoginResponse;
import com.deng.article.dto.UserVO;
import com.deng.article.entity.User;
import com.deng.article.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public UserVO register(String username, String password, String nickname) {
        // 1. 查重
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new BizException("用户名已存在");
        }
        // 2. 密码加密后入库（绝不存明文！）
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        userMapper.insert(user);
        return new UserVO(user.getId(), user.getUsername(), user.getNickname());
    }

    public LoginResponse login(String username, String password) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        // 用户不存在 或 密码不匹配 → 同一个错误提示（防撞库）
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        String token = jwtUtil.createToken(user.getId(), user.getUsername());
        return new LoginResponse(token, new UserVO(user.getId(), user.getUsername(), user.getNickname()));
    }
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        return user;
    }
}