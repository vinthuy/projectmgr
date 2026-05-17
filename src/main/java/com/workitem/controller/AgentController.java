package com.workitem.controller;

import com.workitem.agent.AgentResponse;
import com.workitem.agent.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    
    // 简单的内存会话存储（生产环境应使用数据库）
    private static final Map<String, List<Map<String, Object>>> sessionStore = new ConcurrentHashMap<>();
    private static final Map<String, String> sessionNames = new ConcurrentHashMap<>();
    
    /**
     * 发送消息获取AI回复
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        String message = (String) request.get("message");
        String sessionId = (String) request.get("sessionId");
        
        if (message == null || message.isEmpty()) {
            return Result.error("消息不能为空");
        }
        
        // 如果没有sessionId，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = "sess_" + System.currentTimeMillis();
            sessionNames.put(sessionId, "新对话 " + (sessionNames.size() + 1));
        }
        
        // 调用Agent服务
        AgentResponse response = agentService.processMessage(message, new HashMap<>());
        
        // 保存用户消息
        saveMessage(sessionId, "user", message, null);
        
        // 保存AI回复
        Map<String, Object> metadata = new HashMap<>();
        if (response.getWorkItems() != null) {
            metadata.put("workItem", response.getWorkItems());
        }
        if (response.getSuggestedActions() != null) {
            metadata.put("suggestedActions", response.getSuggestedActions());
        }
        saveMessage(sessionId, "assistant", response.getContent(), metadata);
        
        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("reply", response.getContent());
        result.put("content", response.getContent());
        result.put("intent", response.getIntent());
        result.put("confidence", response.getConfidence());
        result.put("result", buildActionResult(response));
        result.put("suggestions", response.getSuggestedActions());
        result.put("sessionId", sessionId);
        
        return Result.success(result);
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> getSessions() {
        List<Map<String, Object>> sessions = sessionNames.entrySet().stream()
            .map(entry -> {
                Map<String, Object> session = new HashMap<>();
                session.put("id", entry.getKey());
                session.put("name", entry.getValue());
                session.put("createdAt", System.currentTimeMillis());
                return session;
            })
            .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessions", sessions);
        
        return Result.success(result);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        sessionStore.remove(sessionId);
        sessionNames.remove(sessionId);
        return Result.success(null);
    }
    
    /**
     * 获取消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public Result<Map<String, Object>> getMessageHistory(@PathVariable String sessionId) {
        List<Map<String, Object>> messages = sessionStore.getOrDefault(sessionId, new ArrayList<>());
        
        Map<String, Object> result = new HashMap<>();
        result.put("messages", messages);
        result.put("sessionId", sessionId);
        
        return Result.success(result);
    }
    
    /**
     * 保存消息到会话
     */
    private void saveMessage(String sessionId, String role, String content, Map<String, Object> metadata) {
        List<Map<String, Object>> messages = sessionStore.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        Map<String, Object> message = new HashMap<>();
        message.put("messageId", "msg_" + System.nanoTime());
        message.put("role", role);
        message.put("content", content);
        message.put("timestamp", System.currentTimeMillis());
        if (metadata != null) {
            message.put("metadata", metadata);
        }
        
        messages.add(message);
    }
    
    /**
     * 构建操作结果数据
     */
    private Map<String, Object> buildActionResult(AgentResponse response) {
        Map<String, Object> actionResult = new HashMap<>();
        
        if (response.getWorkItems() != null && !response.getWorkItems().isEmpty()) {
            actionResult.put("items", response.getWorkItems());
            if (response.getWorkItems().size() == 1) {
                actionResult.putAll(response.getWorkItems().get(0));
            }
        }
        
        // 如果有统计信息，添加到结果中
        if (response.getContent() != null && response.getContent().contains("总计")) {
            // 简单解析统计信息（实际应该从工具执行结果中获取）
            actionResult.put("total", response.getWorkItems() != null ? response.getWorkItems().size() : 0);
        }
        
        return actionResult;
    }
}
