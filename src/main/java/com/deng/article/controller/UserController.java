package com.deng.article.controller;

import com.deng.article.common.Result;
import com.deng.article.dto.UserVO;
import com.deng.article.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public Result<UserVO> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var user = userService.getById(userId);
        return Result.ok(new UserVO(user.getId(), user.getUsername(), user.getNickname()));
    }
}