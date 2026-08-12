package com.deng.article.agent;

import com.deng.article.llm.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 资料检索 Agent：基于大模型生成写作要点与资料线索，供写作 Agent 参考
 */
@Slf4j
@Component
public class RetrievalAgent implements Agent {

    private final LlmClient llmClient;

    public RetrievalAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String name() {
        return "retrieval";
    }

    @Override
    public void run(AgentContext ctx, Consumer<String> onDelta) {
        String system = "你是一名信息检索专家。请针对用户给出的文章主题，输出 3-5 条关键写作要点与资料线索，"
                + "每条一行，简洁、具体，不输出其他内容。";
        String user = "文章主题：" + ctx.getTopic();
        String notes = llmClient.chat(List.of(
                LlmClient.ChatMessage.system(system),
                LlmClient.ChatMessage.user(user)));
        ctx.setRetrievalNotes(notes.trim());
        log.info("[Agent:retrieval] 检索要点生成完成");
    }
}