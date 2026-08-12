package com.deng.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateRequest {

    /** 为空则自动新建会话 */
    private Long sessionId;

    @NotBlank(message = "主题不能为空")
    @Size(max = 100, message = "主题最长 100 字")
    private String topic;

    /** 文章风格/额外要求，可选 */
    @Size(max = 200, message = "要求最长 200 字")
    private String style;

    /** 编排链配置，如 "retrieval,writing,review"；默认 writing */
    private String agents;
}