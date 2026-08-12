package com.deng.article.config;

import com.deng.article.llm.DeepSeekClient;
import com.deng.article.llm.LlmClient;
import com.deng.article.llm.MockLlmClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 大模型客户端装配：配置了 DEEPSEEK_API_KEY 走真实调用，否则自动降级 Mock（演示模式）
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Bean
    public LlmClient llmClient(@Value("${app.deepseek.api-key:}") String apiKey,
                               @Value("${app.deepseek.base-url}") String baseUrl,
                               @Value("${app.deepseek.model}") String model) {
        if (StringUtils.hasText(apiKey)) {
            log.info("大模型客户端：DeepSeek 真实调用模式（{}）", model);
            return new DeepSeekClient(apiKey, baseUrl, model);
        }
        log.warn("未配置 DEEPSEEK_API_KEY，切换为 Mock 演示模式");
        return new MockLlmClient();
    }
}