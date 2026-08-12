package com.deng.article.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deng.article.common.Result;
import com.deng.article.entity.ChatSession;
import com.deng.article.service.SessionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    private Long userId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @PostMapping
    public Result<ChatSession> create(HttpServletRequest request, @RequestParam String title) {
        return Result.ok(sessionService.create(userId(request), title));
    }

    @GetMapping("/list")
    public Result<Page<ChatSession>> list(HttpServletRequest request,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "20") long size) {
        return Result.ok(sessionService.page(userId(request), page, size));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(HttpServletRequest request, @PathVariable Long id) {
        return Result.ok(sessionService.detail(userId(request), id));
    }
}