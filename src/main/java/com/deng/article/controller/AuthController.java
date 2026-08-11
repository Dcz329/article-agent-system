package com.deng.article.controller;

import com.deng.article.common.Result;
import com.deng.article.dto.LoginRequest;
import com.deng.article.dto.LoginResponse;
import com.deng.article.dto.RegisterRequest;
import com.deng.article.dto.UserVO;
import com.deng.article.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req.getUsername(), req.getPassword(), req.getNickname()));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req.getUsername(), req.getPassword()));
    }
}