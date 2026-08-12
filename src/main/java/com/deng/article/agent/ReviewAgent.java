package com.deng.article.agent;

import com.deng.article.llm.LlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 审校 Agent：对已生成全文进行质量检查，输出审校意见并追加到文章末尾
 */
@Slf4j
@Component
public class ReviewAgent implements Agent {

    private final LlmClient llmClient;

    public ReviewAgent(LlmClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public String name() {
        return "review";
    }

    @Override
    public void run(AgentContext ctx, Consumer<String> onDelta) {
        String system = "你是一名资深编辑，请对用户提供的文章进行审校：检查结构完整性、逻辑连贯性、语言表达，"
                + "指出不足并给出修改建议。输出格式：【审校意见】…，控制在 150 字以内。";
        String user = "文章内容如下：\n" + ctx.getFullText();
        String review = llmClient.chat(List.of(
                LlmClient.ChatMessage.system(system),
                LlmClient.ChatMessage.user(user)));
        ctx.append("\n\n" + review.trim());
        onDelta.accept("\n\n" + review.trim());
        log.info("[Agent:review] 审校意见生成完成");
    }
}