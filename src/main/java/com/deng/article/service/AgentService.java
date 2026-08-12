package com.deng.article.service;

import com.deng.article.agent.AgentContext;
import com.deng.article.agent.AgentOrchestrator;
import com.deng.article.dto.GenerateRequest;
import com.deng.article.entity.Article;
import com.deng.article.entity.ChatSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 创作服务：编排 Agent 链路，通过 SSE 将流式内容推送给前端，完成后落库
 */
@Slf4j
@Service
public class AgentService {

    private final AgentOrchestrator orchestrator;
    private final SessionService sessionService;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    public AgentService(AgentOrchestrator orchestrator, SessionService sessionService) {
        this.orchestrator = orchestrator;
        this.sessionService = sessionService;
    }

    public SseEmitter generate(GenerateRequest req, Long userId) {
        // 不设超时（0L），由 complete() 显式结束
        SseEmitter emitter = new SseEmitter(0L);
        // 生成是耗时操作，必须丢到独立线程，否则会阻塞请求线程
        executor.execute(() -> runGenerate(req, userId, emitter));
        return emitter;
    }

    private void runGenerate(GenerateRequest req, Long userId, SseEmitter emitter) {
        String title = req.getTopic().length() > 50 ? req.getTopic().substring(0, 50) : req.getTopic();
        try {
            List<String> agentNames = orchestrator.resolve(req.getAgents());

            // 1. 会话：无则新建，并把 sessionId 推给前端
            Long sessionId = req.getSessionId();
            if (sessionId == null) {
                ChatSession session = sessionService.create(userId, title);
                sessionId = session.getId();
                emitter.send(SseEmitter.event().name("session").data(Map.of("sessionId", sessionId)));
            }

            // 2. 用户消息落库
            sessionService.saveMessage(sessionId, "user", req.getTopic());

            // 3. 通知前端本次编排链
            emitter.send(SseEmitter.event().name("agents").data(Map.of("agents", String.join("->", agentNames))));

            // 4. 执行编排：每个 chunk 通过 SSE 推给前端，同时累积全文
            AgentContext ctx = new AgentContext(req.getTopic(), req.getStyle());
            String flow = orchestrator.execute(agentNames, ctx, chunk -> {
                try {
                    emitter.send(SseEmitter.event().name("delta").data(Map.of("content", chunk)));
                } catch (Exception e) {
                    throw new IllegalStateException("SSE 推送失败", e);
                }
            });

            // 5. 事务落库：assistant 消息 + 文章
            Article article = sessionService.saveGeneration(sessionId, userId, ctx.getFullText(), flow, title);

            // 6. 通知前端完成
            emitter.send(SseEmitter.event().name("done").data(Map.of("articleId", article.getId())));
            emitter.complete();
        } catch (Exception e) {
            log.error("生成失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message",
                        e.getMessage() == null ? "生成失败" : e.getMessage())));
            } catch (Exception ignored) {
                // emitter 可能已断开
            }
            emitter.complete();
        }
    }
}