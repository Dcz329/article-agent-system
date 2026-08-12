package com.deng.article.agent;

import java.util.function.Consumer;

/**
 * Agent 抽象：编排链中的最小执行单元，新 Agent 只需实现本接口并注册为 Spring Bean
 */
public interface Agent {

    /** Agent 唯一标识，用于编排链配置，如 retrieval / writing / review */
    String name();

    /** 执行任务；onDelta 是流式内容回调，供 SSE 推送 */
    void run(AgentContext ctx, Consumer<String> onDelta);
}