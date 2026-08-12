package com.deng.article.agent;

import com.deng.article.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Agent 编排器：根据配置的链路按序执行各 Agent，并记录实际执行链
 */
@Slf4j
@Component
public class AgentOrchestrator {

    private final Map<String, Agent> agentRegistry = new LinkedHashMap<>();

    /** Spring 会把所有 Agent 实现类自动注入进来（面向接口收集） */
    public AgentOrchestrator(List<Agent> agents) {
        agents.forEach(a -> agentRegistry.put(a.name(), a));
    }

    public String execute(List<String> agentNames, AgentContext ctx, Consumer<String> onDelta) {
        List<String> actual = new ArrayList<>();
        for (String name : agentNames) {
            Agent agent = agentRegistry.get(name);
            if (agent == null) {
                throw new BizException("未知 Agent: " + name);
            }
            ctx.markExecuted(name);
            actual.add(name);
            log.info("开始执行 Agent: {}", name);
            agent.run(ctx, onDelta);
        }
        return String.join("->", actual);
    }

    /** 解析编排链配置：默认写作，逗号分隔去重 */
    public List<String> resolve(String agentsConfig) {
        if (agentsConfig == null || agentsConfig.isBlank()) {
            return List.of("writing");
        }
        List<String> names = new ArrayList<>();
        for (String s : agentsConfig.split(",")) {
            String name = s.trim();
            if (!name.isEmpty() && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}