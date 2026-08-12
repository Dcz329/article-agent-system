package com.deng.article.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * DeepSeek 大模型客户端（OpenAI 兼容接口）
 * 流式：JDK HttpClient 逐行读取 SSE 响应，边读边回调
 */
@Slf4j
public class DeepSeekClient implements LlmClient {

    private static final String CHAT_PATH = "/chat/completions";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String baseUrl;
    private final String model;

    public DeepSeekClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void streamChat(List<ChatMessage> messages, Consumer<String> onDelta) {
        try {
            HttpRequest request = buildRequest(messages, true);
            HttpResponse<Stream<String>> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofLines());
            if (response.statusCode() != 200) {
                String err = response.body().findFirst().orElse("");
                throw new IllegalStateException("DeepSeek API 返回 " + response.statusCode() + ": " + err);
            }
            try (Stream<String> lines = response.body()) {
                lines.forEach(line -> {
                    if (!line.startsWith("data:")) {
                        return;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        return;
                    }
                    try {
                        JsonNode node = objectMapper.readTree(data);
                        JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                        if (delta.isTextual() && !delta.asText().isEmpty()) {
                            onDelta.accept(delta.asText());   // ← 每拿到一小段就推出去
                        }
                    } catch (Exception e) {
                        log.warn("解析流式响应失败: {}", e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 流式调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String chat(List<ChatMessage> messages) {
        try {
            HttpRequest request = buildRequest(messages, false);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("DeepSeek API 返回 " + response.statusCode() + ": " + response.body());
            }
            JsonNode node = objectMapper.readTree(response.body());
            return node.path("choices").path(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new IllegalStateException("DeepSeek 调用失败: " + e.getMessage(), e);
        }
    }

    private HttpRequest buildRequest(List<ChatMessage> messages, boolean stream) throws Exception {
        String body = objectMapper.createObjectNode()
                .put("model", model)
                .put("stream", stream)
                .put("temperature", 0.7)
                .set("messages", objectMapper.valueToTree(
                        messages.stream().map(m -> objectMapper.createObjectNode()
                                        .put("role", m.role())
                                        .put("content", m.content()))
                                .toList()))
                .toString();
        return HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + CHAT_PATH))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }
}