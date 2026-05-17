package com.workitem.agent;

import com.workitem.entity.AgentLearningLog;
import com.workitem.mapper.AgentLearningLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
public class WorkItemAgent {
    
    private final IntentParser intentParser;
    private final ToolExecutor toolExecutor;
    private final ResponseOptimizer responseOptimizer;
    private final ContextManager contextManager;
    private final AgentLearningLogMapper learningLogMapper;
    
    public WorkItemAgent(IntentParser intentParser, 
                        ToolExecutor toolExecutor,
                        ResponseOptimizer responseOptimizer,
                        ContextManager contextManager,
                        AgentLearningLogMapper learningLogMapper) {
        this.intentParser = intentParser;
        this.toolExecutor = toolExecutor;
        this.responseOptimizer = responseOptimizer;
        this.contextManager = contextManager;
        this.learningLogMapper = learningLogMapper;
    }
    
    /**
     * ReAct主循环: Feedback -> Action -> Optimize
     */
    public AgentResponse process(String sessionId, String userInput, Map<String, Object> context) {
        log.info("Agent处理请求: sessionId={}, input={}", sessionId, userInput);
        
        try {
            // Phase 1: Feedback - 解析用户意图
            IntentResult intent = intentParser.parse(userInput, context);
            log.debug("识别意图: {}", intent);
            
            // Phase 2: Action - 执行对应动作
            ActionResult action = toolExecutor.execute(intent);
            log.debug("执行结果: {}", action);
            
            // Phase 3: Optimize - 优化响应
            AgentResponse response = responseOptimizer.optimize(intent, action, context);
            log.debug("优化后响应: {}", response);
            
            // 记录学习日志
            recordLearning(sessionId, userInput, intent, action, response);
            
            // 更新上下文
            updateContext(sessionId, intent, response);
            
            return response;
        } catch (Exception e) {
            log.error("Agent处理异常: sessionId={}", sessionId, e);
            AgentResponse errorResponse = new AgentResponse();
            errorResponse.setContent("❌ 处理失败: " + e.getMessage());
            errorResponse.setIntent("ERROR");
            errorResponse.setConfidence(0.0);
            return errorResponse;
        }
    }
    
    private void recordLearning(String sessionId, String input, 
                                IntentResult intent, ActionResult action, 
                                AgentResponse response) {
        try {
            AgentLearningLog log = new AgentLearningLog();
            log.setSessionId(sessionId);
            log.setUserInput(input);
            log.setDetectedIntent(intent.getIntent());
            log.setActionTaken(action.isSuccess() ? "SUCCESS" : "FAILED");
            log.setResultSuccess(action.isSuccess());
            log.setOptimizationApplied("confidence=" + intent.getConfidence());
            log.setCreatedAt(LocalDateTime.now());
            
            learningLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录学习日志失败", e);
        }
    }
    
    private void updateContext(String sessionId, IntentResult intent, AgentResponse response) {
        Map<String, Object> context = Map.of(
            "lastIntent", intent.getIntent(),
            "lastConfidence", intent.getConfidence(),
            "lastSuccess", response.getContent().contains("✅")
        );
        contextManager.addContext(sessionId, context);
    }
}
