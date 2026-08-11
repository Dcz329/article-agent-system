-- 多智能体文章创作系统 建表脚本
-- 数据库：ai_article（已创建，utf8mb4）
USE ai_article;

CREATE TABLE IF NOT EXISTS `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS `chat_session` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT      NOT NULL COMMENT '所属用户',
    `title`      VARCHAR(100) NOT NULL COMMENT '会话标题',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='会话表';

CREATE TABLE IF NOT EXISTS `message` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT      NOT NULL COMMENT '所属会话',
    `role`       VARCHAR(20) NOT NULL COMMENT '消息角色：user / assistant',
    `content`    TEXT        NOT NULL COMMENT '消息内容',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='消息表';

CREATE TABLE IF NOT EXISTS `article` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `session_id` BIGINT      NOT NULL COMMENT '所属会话',
    `user_id`    BIGINT      NOT NULL COMMENT '所属用户',
    `title`      VARCHAR(200) NOT NULL COMMENT '文章标题',
    `content`    LONGTEXT    NOT NULL COMMENT '文章正文',
    `agent_flow` VARCHAR(255) DEFAULT NULL COMMENT '实际执行的 Agent 编排链路',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='文章表';
