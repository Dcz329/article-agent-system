package com.deng.article.llm;

import java.util.List;
import java.util.function.Consumer;

/**
 * 大模型客户端抽象：支持流式与非流式两种调用
 */
public interface LlmClient {

    /** OpenAI 兼容消息结构 */
    record ChatMessage(String role, String content) {

        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage system(String content) {
            return new ChatMessage("system", content);
        }
    }

    /** 流式对话：每生成一段内容回调一次 onDelta */
    void streamChat(List<ChatMessage> messages, Consumer<String> onDelta);

    /** 非流式对话：一次性返回完整结果（审校 Agent 等场景） */
    String chat(List<ChatMessage> messages);
}