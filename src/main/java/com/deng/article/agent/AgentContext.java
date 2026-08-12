package com.deng.article.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 执行上下文：在编排链各环节间传递主题、风格与中间产物
 */
public class AgentContext {

    private final String topic;
    private final String style;
    private final StringBuilder fullText = new StringBuilder();
    private String retrievalNotes = "";
    private final List<String> executedAgents = new ArrayList<>();

    public AgentContext(String topic, String style) {
        this.topic = topic;
        this.style = style == null ? "" : style;
    }

    public String getTopic() { return topic; }

    public String getStyle() { return style; }

    public String getRetrievalNotes() { return retrievalNotes; }

    public void setRetrievalNotes(String retrievalNotes) { this.retrievalNotes = retrievalNotes; }

    public void append(String text) { fullText.append(text); }

    public String getFullText() { return fullText.toString(); }

    public void markExecuted(String agentName) { executedAgents.add(agentName); }

    public List<String> getExecutedAgents() { return executedAgents; }
}