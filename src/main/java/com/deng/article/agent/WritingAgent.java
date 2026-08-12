package com.deng.article.agent;

import com.deng.article.llm.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 写作 Agent：基于主题与检索要点流式生成文章正文
 */
@Slf4j
@Component
public class WritingAgent implements Agent {

    private final LlmClient llmClient;

    public WritingAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String name() {
        return "writing";
    }

    @Override
    public void run(AgentContext ctx, Consumer<String> onDelta) {
        String system = "你是一名资深中文技术文章作者，擅长撰写结构清晰、逻辑严谨、通俗易懂的科普与技术文章。"
                + "文章需包含标题层级（## 一、…），正文以 Markdown 格式输出。";
        StringBuilder user = new StringBuilder();
        user.append("请围绕以下主题撰写一篇完整的文章：\n").append(ctx.getTopic()).append("\n");
        if (!ctx.getStyle().isEmpty()) {
            user.append("写作风格与额外要求：").append(ctx.getStyle()).append("\n");
        }
        if (!ctx.getRetrievalNotes().isEmpty()) {
            user.append("以下为检索要点，请融入文章：\n").append(ctx.getRetrievalNotes()).append("\n");
        }
        user.append("篇幅 800-1500 字，直接输出正文，不要输出多余说明。");

        // 流式生成：每段内容回调 onDelta（SSE 推给前端）+ 累积进上下文
        llmClient.streamChat(List.of(
                LlmClient.ChatMessage.system(system),
                LlmClient.ChatMessage.user(user.toString())), chunk -> {
            ctx.append(chunk);
            onDelta.accept(chunk);
        });
        log.info("[Agent:writing] 文章生成完成，共 {} 字符", ctx.getFullText().length());
    }
}