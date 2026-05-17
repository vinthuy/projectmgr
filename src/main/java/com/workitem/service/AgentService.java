package com.workitem.service;

import com.workitem.agent.AgentResponse;
import com.workitem.agent.WorkItemAgent;
import com.workitem.dto.*;
import com.workitem.entity.AgentMessage;
import com.workitem.entity.AgentSession;
import com.workitem.mapper.AgentMessageMapper;
import com.workitem.mapper.AgentSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AgentService {
    
    private final WorkItemAgent agent;
    private final AgentSessionMapper sessionMapper;
    private final AgentMessageMapper messageMapper;
    
    public AgentService(WorkItemAgent agent,
                       AgentSessionMapper sessionMapper,
                       AgentMessageMapper messageMapper) {
        this.agent = agent;
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }
    
    /**
     * 处理对话消息
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // 1. 获取或创建会话
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = createSession(request.getMetadata());
        }
        
        // 2. 保存用户消息
        String userMessageId = saveMessage(sessionId, "user", request.getMessage(), null);
        
        // 3. 获取会话上下文
        Map<String, Object> context = buildContext(sessionId);
        
        // 4. Agent处理
        AgentResponse agentResponse = 
            agent.process(sessionId, request.getMessage(), context);
        
        // 5. 保存助手消息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("intent", agentResponse.getIntent());
        metadata.put("confidence", agentResponse.getConfidence());
        metadata.put("suggestedActions", agentResponse.getSuggestedActions());
        
        String assistantMessageId = saveMessage(
            sessionId, "assistant", agentResponse.getContent(), metadata);
        
        // 6. 构建响应
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setMessageId(assistantMessageId);
        response.setRole("assistant");
        response.setContent(agentResponse.getContent());
        response.setWorkItems(agentResponse.getWorkItems());
        response.setIntent(agentResponse.getIntent());
        response.setConfidence(agentResponse.getConfidence());
        response.setMetadata(metadata);
        
        return response;
    }
    
    /**
     * 创建新会话
     */
    public String createSession(Map<String, Object> metadata) {
        String sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "");
        
        AgentSession session = new AgentSession();
        session.setSessionId(sessionId);
        session.setTitle((String) metadata.getOrDefault("title", "新会话"));
        session.setTenantId(metadata.get("tenantId") != null ? 
            ((Number) metadata.get("tenantId")).longValue() : 1L);
        session.setUserId((String) metadata.get("userId"));
        session.setIsActive(true);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        
        sessionMapper.insert(session);
        log.info("创建新会话: sessionId={}", sessionId);
        
        return sessionId;
    }
    
    /**
     * 保存消息
     */
    private String saveMessage(String sessionId, String role, String content, 
                               Map<String, Object> metadata) {
        String messageId = "msg_" + UUID.randomUUID().toString().replace("-", "");
        
        AgentMessage message = new AgentMessage();
        message.setSessionId(sessionId);
        message.setMessageId(messageId);
        message.setRole(role);
        message.setContent(content);
        message.setMetadata(metadata);
        message.setCreatedAt(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        return messageId;
    }
    
    /**
     * 构建上下文
     */
    private Map<String, Object> buildContext(String sessionId) {
        // 从最近的消息中提取上下文信息
        List<AgentMessage> recentMessages = messageMapper.selectRecentMessages(sessionId, 5);
        
        Map<String, Object> context = new HashMap<>();
        for (AgentMessage msg : recentMessages) {
            if (msg.getMetadata() != null) {
                context.putAll(msg.getMetadata());
            }
        }
        
        return context;
    }
    
    /**
     * 获取对话历史
     */
    public Map<String, Object> getHistory(String sessionId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AgentMessage> messages = messageMapper.selectBySessionId(sessionId, page, pageSize);
        long total = messageMapper.countBySessionId(sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("messages", messages);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }
    
    /**
     * 提交反馈
     */
    @Transactional
    public void submitFeedback(FeedbackRequest request) {
        AgentMessage message = messageMapper.selectByMessageId(request.getMessageId());
        if (message == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        
        message.setFeedbackScore(request.getScore());
        message.setFeedbackComment(request.getComment());
        messageMapper.updateById(message);
        
        log.info("收到反馈: messageId={}, score={}", request.getMessageId(), request.getScore());
    }
    
    /**
     * 获取会话列表
     */
    public Map<String, Object> getSessionList(Long tenantId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<AgentSession> sessions = sessionMapper.selectByTenantId(tenantId, page, pageSize);
        long total = sessionMapper.countByTenantId(tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessions", sessions);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        
        return result;
    }
}
