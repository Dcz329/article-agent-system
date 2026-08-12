package com.deng.article.llm;

import java.util.List;
import java.util.function.Consumer;

/**
 * Mock 实现：未配置 API Key 时用于演示完整流程，模拟流式输出效果
 */
public class MockLlmClient implements LlmClient {

    @Override
    public void streamChat(List<ChatMessage> messages, Consumer<String> onDelta) {
        String[] chunks = {
                "这是一篇由「多智能体文章创作系统」原型生成的演示文章。\n\n",
                "## 一、背景与意义\n",
                "随着大语言模型能力的快速提升，AI 辅助创作已成为内容生产的重要方式。",
                "本系统通过多个智能体协作，将写作任务拆解为检索、写作、审校等环节。\n\n",
                "## 二、系统设计\n",
                "系统采用 Spring Boot 3 构建后端，通过 SSE 实现内容流式输出，",
                "前端逐字展示，显著降低了用户等待感。\n\n",
                "## 三、总结\n",
                "原型已验证核心链路（登录鉴权 → 会话管理 → Agent 编排 → 流式输出）的可行性。"
        };
        for (String chunk : chunks) {
            onDelta.accept(chunk);
            try {
                Thread.sleep(80);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        return "【审校结果】文章结构完整、逻辑清晰，语言表达通顺，建议补充具体案例与数据支撑。";
    }
}