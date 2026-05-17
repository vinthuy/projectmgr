package com.workitem.controller;

import com.workitem.agent.AgentResponse;
import com.workitem.agent.WorkItemAgent;
import com.workitem.dto.AgentChatRequest;
import com.workitem.dto.ChatRequest;
import com.workitem.dto.ChatResponse;
import com.workitem.dto.FeedbackRequest;
import com.workitem.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final WorkItemAgent agent;
    private final AgentService agentService;
    
    /**
     * 发送消息获取AI回复 (使用Service层)
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Validated @RequestBody ChatRequest request) {
        ChatResponse response = agentService.chat(request);
        return Result.success(response);
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> getSessions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        // TODO: 从上下文获取tenantId
        Long tenantId = 1L;
        Map<String, Object> result = agentService.getSessionList(tenantId, page, pageSize);
        return Result.success(result);
    }
    
    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        // TODO: 实现删除逻辑
        return Result.success(null);
    }
    
    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Map<String, Object> history = agentService.getHistory(sessionId, page, pageSize);
        return Result.success(history);
    }
    
    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Validated @RequestBody FeedbackRequest request) {
        agentService.submitFeedback(request);
        return Result.success(null);
    }
}
