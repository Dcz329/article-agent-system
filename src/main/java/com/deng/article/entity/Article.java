package com.deng.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long userId;
    private String title;
    private String content;
    /** 实际执行的 Agent 编排链路，如 retrieval->writing->review */
    private String agentFlow;
    private LocalDateTime createdAt;
}