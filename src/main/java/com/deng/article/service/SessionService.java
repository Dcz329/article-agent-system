package com.deng.article.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.deng.article.common.BizException;
import com.deng.article.entity.Article;
import com.deng.article.entity.ChatSession;
import com.deng.article.entity.Message;
import com.deng.article.mapper.ArticleMapper;
import com.deng.article.mapper.ChatSessionMapper;
import com.deng.article.mapper.MessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SessionService {

    private final ChatSessionMapper sessionMapper;
    private final MessageMapper messageMapper;
    private final ArticleMapper articleMapper;

    public SessionService(ChatSessionMapper sessionMapper,
                          MessageMapper messageMapper,
                          ArticleMapper articleMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.articleMapper = articleMapper;
    }

    public ChatSession create(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        sessionMapper.insert(session);
        return session;
    }

    /** 会话分页列表（按最近更新倒序） */
    public Page<ChatSession> page(Long userId, long page, long size) {
        return sessionMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdatedAt));
    }

    public void saveMessage(Long sessionId, String role, String content) {
        Message message = new Message();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        messageMapper.insert(message);
    }

    /**
     * 生成结果落库（assistant 消息 + 文章），@Transactional 保证原子性：
     * 消息插入失败则文章也不会插入，不留半截数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Article saveGeneration(Long sessionId, Long userId, String content, String flow, String title) {
        saveMessage(sessionId, "assistant", content);
        Article article = new Article();
        article.setSessionId(sessionId);
        article.setUserId(userId);
        article.setTitle(title);
        article.setContent(content);
        article.setAgentFlow(flow);
        articleMapper.insert(article);
        return article;
    }

    /** 会话详情：消息历史 + 最新文章（供前端回放） */
    public Map<String, Object> detail(Long userId, Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BizException("会话不存在");
        }
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getSessionId, sessionId)
                        .orderByAsc(Message::getId));
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getSessionId, sessionId)
                        .orderByDesc(Article::getId)
                        .last("LIMIT 1"));
        return Map.of("session", session, "messages", messages, "article", article);
    }
}