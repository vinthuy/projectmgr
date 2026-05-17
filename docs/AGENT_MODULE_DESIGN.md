# Agent模块 - CodeArts Req工作项智能助手设计文档

## 1. 概述

### 1.1 功能描述
创建一个基于AgentScope Java框架的智能Agent模块，支持用户通过自然语言对话方式对华为云CodeArts Req工作项进行增删改查操作。采用"持续反馈-动作-优化"模式实现智能交互。

### 1.2 核心价值
- **自然语言交互**: 用户无需了解API细节，直接用自然语言提问
- **智能意图识别**: 自动识别用户意图(查询/创建/更新/删除)
- **持续优化**: 根据用户反馈不断优化响应质量
- **统一接口**: 封装CodeArts Req API，提供简洁的对话式接口

### 1.3 技术栈
- **Agent框架**: AgentScope Java (阿里云开源的多Agent协作框架)
- **后端**: Spring Boot 3.4.4 + Java 21
- **前端**: Vue 3 + Element Plus
- **外部API**: 华为云CodeArts Req REST API

---

## 2. 系统架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        前端界面层                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          Agent Chat UI (Vue 3 Component)             │   │
│  │  - 对话历史展示                                        │   │
│  │  - 消息输入框                                          │   │
│  │  - 工作项卡片展示                                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓ HTTP/WebSocket
┌─────────────────────────────────────────────────────────────┐
│                      Controller层                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         AgentController                               │   │
│  │  - POST /api/agent/chat      (发送消息)               │   │
│  │  - GET  /api/agent/history   (获取历史)               │   │
│  │  - POST /api/agent/feedback  (提交反馈)               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      Service层                                │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         AgentService                                  │   │
│  │  - 管理Agent生命周期                                   │   │
│  │  - 协调ReAct流程                                       │   │
│  │  - 维护对话上下文                                      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Agent核心层 (AgentScope)                    │
│  ┌──────────────────────────────────────────────────────┐   │
│  │    WorkItemAgent (ReAct Agent)                        │   │
│  │                                                       │   │
│  │  ┌─────────────┐  ┌──────────┐  ┌────────────────┐  │   │
│  │  │  Feedback   │→ │  Action  │→ │   Optimize     │  │   │
│  │  │  (反馈解析)  │  │ (执行动作)│  │  (优化策略)     │  │   │
│  │  └─────────────┘  └──────────┘  └────────────────┘  │   │
│  │       ↑                ↓                  ↑           │   │
│  │       └────────────────┴──────────────────┘           │   │
│  │              持续循环优化                               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                   Tool层 (工具调用)                           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────┐    │
│  │ QueryTool    │ │ CreateTool   │ │ UpdateTool       │    │
│  │ (查询工作项)  │ │ (创建工作项)  │ │ (更新工作项)      │    │
│  └──────────────┘ └──────────────┘ └──────────────────┘    │
│  ┌──────────────┐ ┌──────────────┐                         │
│  │ DeleteTool   │ │ AnalyzeTool  │                         │
│  │ (删除工作项)  │ │ (分析统计)    │                         │
│  └──────────────┘ └──────────────┘                         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                 CodeArts Req API Client                      │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  CodeArtsReqClient                                    │   │
│  │  - 封装华为云API调用                                   │   │
│  │  - 处理认证、重试、错误                                 │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              ↓
                    华为云CodeArts Req服务
```

### 2.2 ReAct模式流程

```
用户输入 → [Feedback: 理解意图] → [Action: 选择工具执行] → [Optimize: 评估结果]
     ↑                                                                  ↓
     └────────────────── 不满意? 重新调整策略 ←─────────────────────────┘
```

**三阶段详细说明:**

1. **Feedback (反馈解析)**
   - 解析用户自然语言输入
   - 提取关键信息: 工作项ID、标题、状态、优先级等
   - 识别意图类型: QUERY/CREATE/UPDATE/DELETE/ANALYZE
   - 维护对话上下文历史

2. **Action (动作执行)**
   - 根据意图选择合适的Tool
   - 调用CodeArts Req API
   - 处理API响应和异常
   - 格式化返回结果

3. **Optimize (优化策略)**
   - 评估执行结果是否符合预期
   - 收集用户显式/隐式反馈
   - 调整后续响应策略
   - 记录学习数据用于改进

---

## 3. 数据库设计

### 3.1 Agent对话历史表

```sql
-- Agent对话会话表
CREATE TABLE agent_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    tenant_id BIGINT NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_agent_session_user ON agent_session(user_id, tenant_id);
CREATE INDEX idx_agent_session_created ON agent_session(created_at DESC);

-- Agent对话消息表
CREATE TABLE agent_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL, -- 'user' | 'assistant' | 'system'
    content TEXT NOT NULL,
    metadata JSONB, -- 存储额外信息: {intent, tools_used, confidence}
    feedback_score INTEGER, -- 用户评分 1-5
    feedback_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_agent_message_session 
        FOREIGN KEY (session_id) REFERENCES agent_session(session_id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_message_session ON agent_message(session_id, created_at);
CREATE INDEX idx_agent_message_role ON agent_message(role);

-- Agent学习记录表 (用于优化)
CREATE TABLE agent_learning_log (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    user_input TEXT,
    detected_intent VARCHAR(50),
    action_taken VARCHAR(100),
    result_success BOOLEAN,
    optimization_applied TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_agent_learning_session 
        FOREIGN KEY (session_id) REFERENCES agent_session(session_id) ON DELETE CASCADE
);

CREATE INDEX idx_agent_learning_intent ON agent_learning_log(detected_intent);
CREATE INDEX idx_agent_learning_success ON agent_learning_log(result_success);
```

---

## 4. API接口设计

### 4.1 RESTful API

#### 4.1.1 发送对话消息
```
POST /api/agent/chat
Request:
{
    "sessionId": "sess_abc123",  // 可选，首次对话不传
    "message": "帮我查询项目PROJ-123的工作项",
    "metadata": {
        "projectId": "PROJ",
        "tenantId": 1
    }
}

Response:
{
    "code": 200,
    "data": {
        "sessionId": "sess_abc123",
        "messageId": "msg_xyz789",
        "role": "assistant",
        "content": "已找到项目PROJ-123的3个工作项...",
        "workItems": [...],  // 结构化数据
        "intent": "QUERY",
        "confidence": 0.95,
        "suggestions": ["查看详情", "筛选状态"]
    }
}
```

#### 4.1.2 获取对话历史
```
GET /api/agent/history?sessionId=sess_abc123&page=1&pageSize=20

Response:
{
    "code": 200,
    "data": {
        "sessionId": "sess_abc123",
        "messages": [
            {
                "messageId": "msg_001",
                "role": "user",
                "content": "查询工作项",
                "createdAt": "2026-04-26T10:00:00"
            },
            {
                "messageId": "msg_002",
                "role": "assistant",
                "content": "找到以下工作项...",
                "workItems": [...],
                "createdAt": "2026-04-26T10:00:01"
            }
        ],
        "total": 50,
        "page": 1,
        "pageSize": 20
    }
}
```

#### 4.1.3 提交反馈
```
POST /api/agent/feedback
Request:
{
    "messageId": "msg_xyz789",
    "score": 5,  // 1-5
    "comment": "回答很准确"
}

Response:
{
    "code": 200,
    "message": "反馈提交成功"
}
```

#### 4.1.4 创建新会话
```
POST /api/agent/session
Request:
{
    "title": "工作项查询会话",
    "tenantId": 1
}

Response:
{
    "code": 200,
    "data": {
        "sessionId": "sess_new123",
        "title": "工作项查询会话",
        "createdAt": "2026-04-26T10:00:00"
    }
}
```

#### 4.1.5 获取会话列表
```
GET /api/agent/sessions?tenantId=1&page=1&pageSize=10

Response:
{
    "code": 200,
    "data": {
        "sessions": [...],
        "total": 25,
        "page": 1,
        "pageSize": 10
    }
}
```

---

## 5. 核心类设计

### 5.1 Entity层

#### AgentSession.java
```java
package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String userId;
    private Long tenantId;
    private String title;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive;
}
```

#### AgentMessage.java
```java
package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_message")
public class AgentMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String messageId;
    private String role; // user, assistant, system
    private String content;
    
    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;
    
    private Integer feedbackScore;
    private String feedbackComment;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

#### AgentLearningLog.java
```java
package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_learning_log")
public class AgentLearningLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String userInput;
    private String detectedIntent;
    private String actionTaken;
    private Boolean resultSuccess;
    private String optimizationApplied;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

### 5.2 DTO层

#### ChatRequest.java
```java
package com.workitem.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class ChatRequest {
    private String sessionId;
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    private Map<String, Object> metadata;
}
```

#### ChatResponse.java
```java
package com.workitem.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String sessionId;
    private String messageId;
    private String role;
    private String content;
    private List<Map<String, Object>> workItems;
    private String intent;
    private Double confidence;
    private List<String> suggestions;
    private Map<String, Object> metadata;
}
```

#### FeedbackRequest.java
```java
package com.workitem.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
public class FeedbackRequest {
    @NotBlank(message = "消息ID不能为空")
    private String messageId;
    
    @Min(1)
    @Max(5)
    private Integer score;
    
    private String comment;
}
```

### 5.3 Agent核心层

#### WorkItemAgent.java (ReAct Agent)
```java
package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class WorkItemAgent {
    
    private final IntentParser intentParser;
    private final ToolExecutor toolExecutor;
    private final ResponseOptimizer responseOptimizer;
    private final ContextManager contextManager;
    
    public WorkItemAgent(IntentParser intentParser, 
                        ToolExecutor toolExecutor,
                        ResponseOptimizer responseOptimizer,
                        ContextManager contextManager) {
        this.intentParser = intentParser;
        this.toolExecutor = toolExecutor;
        this.responseOptimizer = responseOptimizer;
        this.contextManager = contextManager;
    }
    
    /**
     * ReAct主循环: Feedback -> Action -> Optimize
     */
    public AgentResponse process(String sessionId, String userInput, Map<String, Object> context) {
        log.info("Agent处理请求: sessionId={}, input={}", sessionId, userInput);
        
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
        
        return response;
    }
    
    private void recordLearning(String sessionId, String input, 
                                IntentResult intent, ActionResult action, 
                                AgentResponse response) {
        // 保存到agent_learning_log表
    }
}
```

#### IntentParser.java (意图解析器)
```java
package com.workitem.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Component
public class IntentParser {
    
    private static final Map<String, Pattern> INTENT_PATTERNS = new HashMap<>();
    
    static {
        INTENT_PATTERNS.put("QUERY", Pattern.compile("(查询|查找|搜索|查看|显示|列出).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("CREATE", Pattern.compile("(创建|新建|添加|生成).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("UPDATE", Pattern.compile("(更新|修改|编辑|变更).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("DELETE", Pattern.compile("(删除|移除|取消).*(工作项|任务|bug|需求)"));
        INTENT_PATTERNS.put("ANALYZE", Pattern.compile("(分析|统计|汇总|报告).*(工作项|任务|进度)"));
    }
    
    public IntentResult parse(String userInput, Map<String, Object> context) {
        IntentResult result = new IntentResult();
        result.setOriginalInput(userInput);
        
        // 1. 识别意图类型
        String intent = detectIntent(userInput);
        result.setIntent(intent);
        
        // 2. 提取实体信息
        Map<String, Object> entities = extractEntities(userInput);
        result.setEntities(entities);
        
        // 3. 计算置信度
        double confidence = calculateConfidence(intent, entities);
        result.setConfidence(confidence);
        
        // 4. 结合上下文
        enrichWithContext(result, context);
        
        log.info("意图解析完成: intent={}, confidence={}, entities={}", 
                 intent, confidence, entities);
        
        return result;
    }
    
    private String detectIntent(String input) {
        for (Map.Entry<String, Pattern> entry : INTENT_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(input).find()) {
                return entry.getKey();
            }
        }
        return "UNKNOWN";
    }
    
    private Map<String, Object> extractEntities(String input) {
        Map<String, Object> entities = new HashMap<>();
        
        // 提取工作项ID (如: PROJ-123)
        Pattern idPattern = Pattern.compile("([A-Z]+-\\d+)");
        Matcher matcher = idPattern.matcher(input);
        if (matcher.find()) {
            entities.put("workItemId", matcher.group(1));
        }
        
        // 提取项目名称
        Pattern projectPattern = Pattern.compile("项目[\\s]*([A-Za-z0-9]+)");
        matcher = projectPattern.matcher(input);
        if (matcher.find()) {
            entities.put("projectKey", matcher.group(1));
        }
        
        // 提取状态
        if (input.contains("进行中")) {
            entities.put("status", "IN_PROGRESS");
        } else if (input.contains("已完成")) {
            entities.put("status", "DONE");
        } else if (input.contains("待处理")) {
            entities.put("status", "TODO");
        }
        
        // 提取优先级
        if (input.contains("高优先级") || input.contains("紧急")) {
            entities.put("priority", "HIGH");
        } else if (input.contains("低优先级")) {
            entities.put("priority", "LOW");
        }
        
        return entities;
    }
    
    private double calculateConfidence(String intent, Map<String, Object> entities) {
        double confidence = 0.5;
        
        if (!"UNKNOWN".equals(intent)) {
            confidence += 0.3;
        }
        
        if (!entities.isEmpty()) {
            confidence += 0.2;
        }
        
        return Math.min(confidence, 1.0);
    }
    
    private void enrichWithContext(IntentResult result, Map<String, Object> context) {
        if (context != null) {
            result.getEntities().putAll(context);
        }
    }
}

@Data
class IntentResult {
    private String intent;
    private Map<String, Object> entities = new HashMap<>();
    private Double confidence;
    private String originalInput;
}
```

#### ToolExecutor.java (工具执行器)
```java
package com.workitem.agent;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class ToolExecutor {
    
    private final CodeArtsReqClient codeArtsClient;
    private final Map<String, Tool> tools = new HashMap<>();
    
    public ToolExecutor(CodeArtsReqClient codeArtsClient) {
        this.codeArtsClient = codeArtsClient;
        registerTools();
    }
    
    private void registerTools() {
        tools.put("QUERY", new QueryTool(codeArtsClient));
        tools.put("CREATE", new CreateTool(codeArtsClient));
        tools.put("UPDATE", new UpdateTool(codeArtsClient));
        tools.put("DELETE", new DeleteTool(codeArtsClient));
        tools.put("ANALYZE", new AnalyzeTool(codeArtsClient));
    }
    
    public ActionResult execute(IntentResult intent) {
        String intentType = intent.getIntent();
        Tool tool = tools.get(intentType);
        
        if (tool == null) {
            return ActionResult.error("不支持的操作类型: " + intentType);
        }
        
        try {
            Object result = tool.execute(intent.getEntities());
            return ActionResult.success(result);
        } catch (Exception e) {
            log.error("工具执行失败: intentType={}", intentType, e);
            return ActionResult.error("执行失败: " + e.getMessage());
        }
    }
}

@Data
class ActionResult {
    private boolean success;
    private Object data;
    private String errorMessage;
    
    public static ActionResult success(Object data) {
        ActionResult result = new ActionResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }
    
    public static ActionResult error(String message) {
        ActionResult result = new ActionResult();
        result.setSuccess(false);
        result.setErrorMessage(message);
        return result;
    }
}

interface Tool {
    Object execute(Map<String, Object> params) throws Exception;
}
```

#### QueryTool.java (查询工具)
```java
package com.workitem.agent.tool;

import com.workitem.agent.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class QueryTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public QueryTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行查询工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        String projectKey = (String) params.get("projectKey");
        String status = (String) params.get("status");
        
        if (workItemId != null) {
            // 查询单个工作项
            return client.getWorkItem(workItemId);
        } else if (projectKey != null) {
            // 查询项目下的工作项列表
            Map<String, Object> filters = new HashMap<>();
            if (status != null) {
                filters.put("status", status);
            }
            return client.listWorkItems(projectKey, filters);
        } else {
            throw new IllegalArgumentException("缺少查询参数: workItemId 或 projectKey");
        }
    }
}
```

#### CreateTool.java (创建工具)
```java
package com.workitem.agent.tool;

import com.workitem.agent.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class CreateTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public CreateTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行创建工具: params={}", params);
        
        String projectKey = (String) params.get("projectKey");
        String title = (String) params.get("title");
        String description = (String) params.get("description");
        String type = (String) params.get("type", "TASK");
        String priority = (String) params.get("priority", "MEDIUM");
        
        if (projectKey == null || title == null) {
            throw new IllegalArgumentException("缺少必要参数: projectKey 和 title");
        }
        
        Map<String, Object> workItem = new HashMap<>();
        workItem.put("projectKey", projectKey);
        workItem.put("title", title);
        workItem.put("description", description);
        workItem.put("type", type);
        workItem.put("priority", priority);
        
        return client.createWorkItem(workItem);
    }
}
```

#### UpdateTool.java (更新工具)
```java
package com.workitem.agent.tool;

import com.workitem.agent.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class UpdateTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public UpdateTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行更新工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        if (workItemId == null) {
            throw new IllegalArgumentException("缺少必要参数: workItemId");
        }
        
        Map<String, Object> updates = new HashMap<>();
        if (params.containsKey("title")) {
            updates.put("title", params.get("title"));
        }
        if (params.containsKey("status")) {
            updates.put("status", params.get("status"));
        }
        if (params.containsKey("priority")) {
            updates.put("priority", params.get("priority"));
        }
        if (params.containsKey("description")) {
            updates.put("description", params.get("description"));
        }
        
        return client.updateWorkItem(workItemId, updates);
    }
}
```

#### DeleteTool.java (删除工具)
```java
package com.workitem.agent.tool;

import com.workitem.agent.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class DeleteTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public DeleteTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行删除工具: params={}", params);
        
        String workItemId = (String) params.get("workItemId");
        if (workItemId == null) {
            throw new IllegalArgumentException("缺少必要参数: workItemId");
        }
        
        client.deleteWorkItem(workItemId);
        return Map.of("success", true, "message", "工作项已删除");
    }
}
```

#### AnalyzeTool.java (分析工具)
```java
package com.workitem.agent.tool;

import com.workitem.agent.CodeArtsReqClient;
import com.workitem.agent.Tool;
import lombok.extern.slf4j.Slf4j;
import java.util.*;

@Slf4j
public class AnalyzeTool implements Tool {
    
    private final CodeArtsReqClient client;
    
    public AnalyzeTool(CodeArtsReqClient client) {
        this.client = client;
    }
    
    @Override
    public Object execute(Map<String, Object> params) throws Exception {
        log.info("执行分析工具: params={}", params);
        
        String projectKey = (String) params.get("projectKey");
        if (projectKey == null) {
            throw new IllegalArgumentException("缺少必要参数: projectKey");
        }
        
        // 获取统计数据
        Map<String, Object> stats = client.getWorkItemStats(projectKey);
        
        // 生成分析报告
        String report = generateReport(stats);
        
        return Map.of(
            "stats", stats,
            "report", report
        );
    }
    
    private String generateReport(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 工作项分析报告\n\n");
        sb.append("总工作项数: ").append(stats.get("total")).append("\n");
        sb.append("已完成: ").append(stats.get("completed")).append("\n");
        sb.append("进行中: ").append(stats.get("inProgress")).append("\n");
        sb.append("完成率: ").append(stats.get("completionRate")).append("%\n");
        return sb.toString();
    }
}
```

#### ResponseOptimizer.java (响应优化器)
```java
package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class ResponseOptimizer {
    
    public AgentResponse optimize(IntentResult intent, ActionResult action, 
                                  Map<String, Object> context) {
        AgentResponse response = new AgentResponse();
        
        if (!action.isSuccess()) {
            // 失败情况: 提供友好的错误提示和建议
            response.setContent("❌ 操作失败: " + action.getErrorMessage());
            response.setSuggestions(getErrorSuggestions(intent));
            return response;
        }
        
        // 成功情况: 格式化输出
        String formattedContent = formatResponse(intent, action.getData());
        response.setContent(formattedContent);
        
        // 添加结构化数据
        if (action.getData() instanceof List) {
            response.setWorkItems((List<Map<String, Object>>) action.getData());
        } else if (action.getData() instanceof Map) {
            response.setMetadata((Map<String, Object>) action.getData());
        }
        
        // 生成建议操作
        response.setSuggestions(generateSuggestions(intent, action.getData()));
        
        // 设置元数据
        response.setIntent(intent.getIntent());
        response.setConfidence(intent.getConfidence());
        
        return response;
    }
    
    private String formatResponse(IntentResult intent, Object data) {
        switch (intent.getIntent()) {
            case "QUERY":
                return formatQueryResponse(data);
            case "CREATE":
                return formatCreateResponse(data);
            case "UPDATE":
                return formatUpdateResponse(data);
            case "DELETE":
                return formatDeleteResponse(data);
            case "ANALYZE":
                return formatAnalyzeResponse(data);
            default:
                return "抱歉,我还没有学会这个操作。";
        }
    }
    
    private String formatQueryResponse(Object data) {
        if (data instanceof Map) {
            // 单个工作项
            Map<String, Object> item = (Map<String, Object>) data;
            return String.format("✅ 找到工作项:\n\n标题: %s\nID: %s\n状态: %s\n优先级: %s",
                item.get("title"), item.get("id"), item.get("status"), item.get("priority"));
        } else if (data instanceof List) {
            // 工作项列表
            List<?> items = (List<?>) data;
            return String.format("✅ 找到 %d 个工作项:\n\n%s", 
                items.size(), formatItemList(items));
        }
        return "未找到相关工作项";
    }
    
    private String formatItemList(List<?> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(items.size(), 5); i++) {
            Map<String, Object> item = (Map<String, Object>) items.get(i);
            sb.append(String.format("%d. [%s] %s (%s)\n", 
                i + 1, item.get("id"), item.get("title"), item.get("status")));
        }
        if (items.size() > 5) {
            sb.append(String.format("\n... 还有 %d 个", items.size() - 5));
        }
        return sb.toString();
    }
    
    private String formatCreateResponse(Object data) {
        Map<String, Object> item = (Map<String, Object>) data;
        return String.format("✅ 工作项创建成功!\n\nID: %s\n标题: %s", 
            item.get("id"), item.get("title"));
    }
    
    private String formatUpdateResponse(Object data) {
        return "✅ 工作项更新成功!";
    }
    
    private String formatDeleteResponse(Object data) {
        return "✅ 工作项已删除";
    }
    
    private String formatAnalyzeResponse(Object data) {
        Map<String, Object> result = (Map<String, Object>) data;
        return (String) result.get("report");
    }
    
    private List<String> generateSuggestions(IntentResult intent, Object data) {
        List<String> suggestions = new ArrayList<>();
        
        switch (intent.getIntent()) {
            case "QUERY":
                suggestions.add("查看详情");
                suggestions.add("筛选状态");
                suggestions.add "导出列表");
                break;
            case "CREATE":
                suggestions.add("继续创建");
                suggestions.add("返回列表");
                break;
            case "UPDATE":
                suggestions.add("再次修改");
                suggestions.add("查看更新");
                break;
            case "ANALYZE":
                suggestions.add("查看详细统计");
                suggestions.add("生成图表");
                break;
        }
        
        return suggestions;
    }
    
    private List<String> getErrorSuggestions(IntentResult intent) {
        return Arrays.asList(
            "重试操作",
            "检查参数",
            "联系管理员"
        );
    }
}

@Data
class AgentResponse {
    private String content;
    private List<Map<String, Object>> workItems;
    private Map<String, Object> metadata;
    private String intent;
    private Double confidence;
    private List<String> suggestions;
}
```

#### ContextManager.java (上下文管理器)
```java
package com.workitem.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ContextManager {
    
    private final Map<String, Deque<Map<String, Object>>> sessionContexts = new ConcurrentHashMap<>();
    private static final int MAX_CONTEXT_SIZE = 10;
    
    public void addContext(String sessionId, Map<String, Object> context) {
        sessionContexts.computeIfAbsent(sessionId, k -> new ArrayDeque<>());
        Deque<Map<String, Object>> contexts = sessionContexts.get(sessionId);
        
        if (contexts.size() >= MAX_CONTEXT_SIZE) {
            contexts.pollFirst(); // 移除最旧的上下文
        }
        
        contexts.addLast(context);
        log.debug("添加上下文: sessionId={}, size={}", sessionId, contexts.size());
    }
    
    public Map<String, Object> getContext(String sessionId) {
        Deque<Map<String, Object>> contexts = sessionContexts.get(sessionId);
        if (contexts == null || contexts.isEmpty()) {
            return new HashMap<>();
        }
        
        // 合并所有上下文
        Map<String, Object> merged = new HashMap<>();
        for (Map<String, Object> ctx : contexts) {
            merged.putAll(ctx);
        }
        
        return merged;
    }
    
    public void clearContext(String sessionId) {
        sessionContexts.remove(sessionId);
        log.debug("清除上下文: sessionId={}", sessionId);
    }
}
```

### 5.4 CodeArts Req客户端

#### CodeArtsReqClient.java
```java
package com.workitem.agent.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;

@Slf4j
@Component
public class CodeArtsReqClient {
    
    @Value("${codearts.api.base-url:https://openapi.huaweicloud.com}")
    private String baseUrl;
    
    @Value("${codearts.api.token:}")
    private String apiToken;
    
    @Value("${codearts.api.project-id:}")
    private String projectId;
    
    /**
     * 查询单个工作项
     */
    public Map<String, Object> getWorkItem(String workItemId) {
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        HttpResponse response = HttpRequest.get(url)
            .header("X-Auth-Token", apiToken)
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("查询工作项失败: " + response.body());
        }
        
        return JSONUtil.toBean(response.body(), Map.class);
    }
    
    /**
     * 查询工作项列表
     */
    public List<Map<String, Object>> listWorkItems(String projectKey, 
                                                    Map<String, Object> filters) {
        String url = String.format("%s/v1/projects/%s/work-items", 
            baseUrl, projectId);
        
        Map<String, Object> queryParams = new HashMap<>();
        if (filters != null) {
            queryParams.putAll(filters);
        }
        
        HttpResponse response = HttpRequest.get(url)
            .header("X-Auth-Token", apiToken)
            .form(queryParams)
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("查询工作项列表失败: " + response.body());
        }
        
        Map<String, Object> result = JSONUtil.toBean(response.body(), Map.class);
        return (List<Map<String, Object>>) result.get("items");
    }
    
    /**
     * 创建工作项
     */
    public Map<String, Object> createWorkItem(Map<String, Object> workItem) {
        String url = String.format("%s/v1/projects/%s/work-items", 
            baseUrl, projectId);
        
        HttpResponse response = HttpRequest.post(url)
            .header("X-Auth-Token", apiToken)
            .body(JSONUtil.toJsonStr(workItem))
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("创建工作项失败: " + response.body());
        }
        
        return JSONUtil.toBean(response.body(), Map.class);
    }
    
    /**
     * 更新工作项
     */
    public Map<String, Object> updateWorkItem(String workItemId, 
                                               Map<String, Object> updates) {
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        HttpResponse response = HttpRequest.put(url)
            .header("X-Auth-Token", apiToken)
            .body(JSONUtil.toJsonStr(updates))
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("更新工作项失败: " + response.body());
        }
        
        return JSONUtil.toBean(response.body(), Map.class);
    }
    
    /**
     * 删除工作项
     */
    public void deleteWorkItem(String workItemId) {
        String url = String.format("%s/v1/projects/%s/work-items/%s", 
            baseUrl, projectId, workItemId);
        
        HttpResponse response = HttpRequest.delete(url)
            .header("X-Auth-Token", apiToken)
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("删除工作项失败: " + response.body());
        }
    }
    
    /**
     * 获取工作项统计
     */
    public Map<String, Object> getWorkItemStats(String projectKey) {
        String url = String.format("%s/v1/projects/%s/work-items/stats", 
            baseUrl, projectId);
        
        HttpResponse response = HttpRequest.get(url)
            .header("X-Auth-Token", apiToken)
            .execute();
        
        if (!response.isOk()) {
            throw new RuntimeException("获取统计失败: " + response.body());
        }
        
        return JSONUtil.toBean(response.body(), Map.class);
    }
}
```

### 5.5 Service层

#### AgentService.java
```java
package com.workitem.service;

import com.workitem.agent.WorkItemAgent;
import com.workitem.dto.*;
import com.workitem.entity.AgentMessage;
import com.workitem.entity.AgentSession;
import com.workitem.mapper.AgentMessageMapper;
import com.workitem.mapper.AgentSessionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.UUID;

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
        com.workitem.agent.AgentResponse agentResponse = 
            agent.process(sessionId, request.getMessage(), context);
        
        // 5. 保存助手消息
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("intent", agentResponse.getIntent());
        metadata.put("confidence", agentResponse.getConfidence());
        metadata.put("suggestions", agentResponse.getSuggestions());
        
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
        response.setSuggestions(agentResponse.getSuggestions());
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
        session.setTenantId((Long) metadata.get("tenantId"));
        session.setIsActive(true);
        
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
```

### 5.6 Mapper层

#### AgentSessionMapper.java
```java
package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.AgentSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AgentSessionMapper extends BaseMapper<AgentSession> {
    
    @Select("SELECT * FROM agent_session WHERE tenant_id = #{tenantId} AND is_active = true ORDER BY created_at DESC LIMIT #{pageSize} OFFSET #{offset}")
    List<AgentSession> selectByTenantId(@Param("tenantId") Long tenantId, 
                                        @Param("page") int page, 
                                        @Param("pageSize") int pageSize);
    
    @Select("SELECT COUNT(*) FROM agent_session WHERE tenant_id = #{tenantId} AND is_active = true")
    long countByTenantId(@Param("tenantId") Long tenantId);
}
```

#### AgentMessageMapper.java
```java
package com.workitem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workitem.entity.AgentMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AgentMessageMapper extends BaseMapper<AgentMessage> {
    
    @Select("SELECT * FROM agent_message WHERE session_id = #{sessionId} ORDER BY created_at ASC LIMIT #{pageSize} OFFSET #{offset}")
    List<AgentMessage> selectBySessionId(@Param("sessionId") String sessionId, 
                                         @Param("page") int page, 
                                         @Param("pageSize") int pageSize);
    
    @Select("SELECT COUNT(*) FROM agent_message WHERE session_id = #{sessionId}")
    long countBySessionId(@Param("sessionId") String sessionId);
    
    @Select("SELECT * FROM agent_message WHERE session_id = #{sessionId} ORDER BY created_at DESC LIMIT #{limit}")
    List<AgentMessage> selectRecentMessages(@Param("sessionId") String sessionId, 
                                            @Param("limit") int limit);
    
    @Select("SELECT * FROM agent_message WHERE message_id = #{messageId}")
    AgentMessage selectByMessageId(@Param("messageId") String messageId);
}
```

### 5.7 Controller层

#### AgentController.java
```java
package com.workitem.controller;

import com.workitem.dto.*;
import com.workitem.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    
    /**
     * 发送对话消息
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        ChatResponse response = agentService.chat(request);
        return Result.success(response);
    }
    
    /**
     * 获取对话历史
     */
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(
            @RequestParam String sessionId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Map<String, Object> history = agentService.getHistory(sessionId, page, pageSize);
        return Result.success(history);
    }
    
    /**
     * 提交反馈
     */
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody FeedbackRequest request) {
        agentService.submitFeedback(request);
        return Result.success(null);
    }
    
    /**
     * 创建新会话
     */
    @PostMapping("/session")
    public Result<Map<String, Object>> createSession(@RequestBody Map<String, Object> request) {
        String sessionId = agentService.createSession(request);
        return Result.success(Map.of("sessionId", sessionId));
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/sessions")
    public Result<Map<String, Object>> getSessionList(
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> sessions = agentService.getSessionList(tenantId, page, pageSize);
        return Result.success(sessions);
    }
}
```

---

## 6. 前端界面设计

### 6.1 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  Agent智能助手                                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  💬 你好!我是工作项智能助手,可以帮你:                   │  │
│  │     • 查询工作项                                       │  │
│  │     • 创建工作项                                       │  │
│  │     • 更新工作项                                       │  │
│  │     • 统计分析                                         │  │
│  │                                                        │  │
│  │  试试说: "查询项目PROJ的工作项"                         │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  👤 帮我查询项目PROJ-123的工作项                        │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  🤖 ✅ 找到 3 个工作项:                                 │  │
│  │                                                        │  │
│  │  1. [PROJ-123] 修复登录bug (已完成)                    │  │
│  │  2. [PROJ-124] 添加用户权限 (进行中)                   │  │
│  │  3. [PROJ-125] 优化性能 (待处理)                       │  │
│  │                                                        │  │
│  │  [查看详情] [筛选状态] [导出列表]                       │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  👤 创建一个新的任务,标题是"测试功能"                    │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  🤖 ✅ 工作项创建成功!                                  │  │
│  │                                                        │  │
│  │  ID: PROJ-126                                          │  │
│  │  标题: 测试功能                                         │  │
│  │                                                        │  │
│  │  [继续创建] [返回列表]                                  │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────────┐  │
│  │  输入消息...                              [发送]       │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 6.2 组件结构

```
frontend/src/views/agent/
├── AgentChat.vue          # 主聊天界面
├── components/
│   ├── MessageBubble.vue  # 消息气泡组件
│   ├── WorkItemCard.vue   # 工作项卡片组件
│   ├── SuggestionChip.vue # 建议操作按钮
│   └── FeedbackStar.vue   # 反馈评分组件
└── api/
    └── agent.js           # API调用封装
```

### 6.3 核心组件代码

#### AgentChat.vue
```vue
<template>
  <div class="agent-chat">
    <!-- 头部 -->
    <div class="chat-header">
      <h2>🤖 Agent智能助手</h2>
      <el-button @click="createNewSession">新会话</el-button>
    </div>
    
    <!-- 消息列表 -->
    <div class="message-list" ref="messageListRef">
      <div v-for="msg in messages" :key="msg.messageId" class="message-item">
        <MessageBubble 
          :message="msg"
          @feedback="handleFeedback"
        />
      </div>
      
      <!-- 加载指示器 -->
      <div v-if="loading" class="loading-indicator">
        <el-skeleton :rows="2" animated />
      </div>
    </div>
    
    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="3"
        placeholder="输入消息... (例如: 查询项目PROJ的工作项)"
        @keyup.enter.ctrl="sendMessage"
      />
      <el-button 
        type="primary" 
        @click="sendMessage"
        :loading="sending"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import MessageBubble from './components/MessageBubble.vue'
import { sendChat, getHistory, createSession } from '@/api/agent'

const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const sending = ref(false)
const sessionId = ref('')
const messageListRef = ref(null)

onMounted(() => {
  loadHistory()
})

const loadHistory = async () => {
  if (!sessionId.value) return
  
  loading.value = true
  try {
    const res = await getHistory(sessionId.value, 1, 50)
    messages.value = res.data.messages
    scrollToBottom()
  } catch (error) {
    ElMessage.error('加载历史消息失败')
  } finally {
    loading.value = false
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  const userMsg = {
    messageId: 'temp_' + Date.now(),
    role: 'user',
    content: inputMessage.value,
    createdAt: new Date().toISOString()
  }
  
  messages.value.push(userMsg)
  const currentInput = inputMessage.value
  inputMessage.value = ''
  sending.value = true
  
  try {
    const res = await sendChat({
      sessionId: sessionId.value,
      message: currentInput
    })
    
    // 更新sessionId
    if (!sessionId.value) {
      sessionId.value = res.data.sessionId
    }
    
    // 添加助手回复
    messages.value.push({
      messageId: res.data.messageId,
      role: 'assistant',
      content: res.data.content,
      workItems: res.data.workItems,
      suggestions: res.data.suggestions,
      createdAt: new Date().toISOString()
    })
    
    scrollToBottom()
  } catch (error) {
    ElMessage.error('发送消息失败: ' + error.message)
  } finally {
    sending.value = false
  }
}

const handleFeedback = async (messageId, score, comment) => {
  try {
    await submitFeedback({ messageId, score, comment })
    ElMessage.success('感谢反馈!')
  } catch (error) {
    ElMessage.error('提交反馈失败')
  }
}

const createNewSession = async () => {
  try {
    const res = await createSession({
      title: '新会话',
      tenantId: 1
    })
    sessionId.value = res.data.sessionId
    messages.value = []
    ElMessage.success('已创建新会话')
  } catch (error) {
    ElMessage.error('创建会话失败')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}
</script>

<style scoped>
.agent-chat {
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.message-item {
  margin-bottom: 16px;
}

.loading-indicator {
  padding: 16px;
}

.input-area {
  padding: 16px;
  border-top: 1px solid #e8e8e8;
  display: flex;
  gap: 12px;
}

.input-area .el-input {
  flex: 1;
}
</style>
```

#### MessageBubble.vue
```vue
<template>
  <div :class="['message-bubble', message.role]">
    <div class="bubble-content">
      <!-- 用户消息 -->
      <div v-if="message.role === 'user'" class="user-message">
        <span class="avatar">👤</span>
        <div class="content">{{ message.content }}</div>
      </div>
      
      <!-- 助手消息 -->
      <div v-else class="assistant-message">
        <span class="avatar">🤖</span>
        <div class="content-wrapper">
          <div class="content">{{ message.content }}</div>
          
          <!-- 工作项列表 -->
          <div v-if="message.workItems && message.workItems.length" class="work-items">
            <WorkItemCard 
              v-for="item in message.workItems" 
              :key="item.id"
              :item="item"
            />
          </div>
          
          <!-- 建议操作 -->
          <div v-if="message.suggestions && message.suggestions.length" class="suggestions">
            <SuggestionChip
              v-for="suggestion in message.suggestions"
              :key="suggestion"
              :label="suggestion"
              @click="handleSuggestionClick(suggestion)"
            />
          </div>
          
          <!-- 反馈评分 -->
          <div class="feedback">
            <FeedbackStar 
              @rate="(score) => emit('feedback', message.messageId, score)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import WorkItemCard from './WorkItemCard.vue'
import SuggestionChip from './SuggestionChip.vue'
import FeedbackStar from './FeedbackStar.vue'

defineProps({
  message: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['feedback'])

const handleSuggestionClick = (suggestion) => {
  // 触发建议操作
  console.log('点击建议:', suggestion)
}
</script>

<style scoped>
.message-bubble {
  display: flex;
  margin-bottom: 16px;
}

.message-bubble.user {
  justify-content: flex-end;
}

.bubble-content {
  max-width: 70%;
}

.user-message, .assistant-message {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.avatar {
  font-size: 24px;
}

.content {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f5f5;
  line-height: 1.6;
}

.user-message .content {
  background: #1890ff;
  color: white;
}

.content-wrapper {
  flex: 1;
}

.work-items {
  margin-top: 12px;
}

.suggestions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.feedback {
  margin-top: 8px;
  text-align: right;
}
</style>
```

---

## 7. 配置文件

### 7.1 application.yml配置

```yaml
# CodeArts Req API配置
codearts:
  api:
    base-url: https://openapi.huaweicloud.com
    token: ${CODEARTS_API_TOKEN:your-api-token}
    project-id: ${CODEARTS_PROJECT_ID:your-project-id}

# Agent配置
agent:
  max-context-size: 10
  max-retry: 3
  timeout: 30000
```

### 7.2 pom.xml依赖

```xml
<!-- AgentScope Java (假设版本号) -->
<dependency>
    <groupId>io.agentscope</groupId>
    <artifactId>agentscope-java</artifactId>
    <version>0.1.0</version>
</dependency>

<!-- Hutool HTTP客户端 (已有) -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>${hutool.version}</version>
</dependency>
```

---

## 8. 实施计划

### 8.1 开发阶段

**阶段1: 基础架构搭建 (1-2天)**
- [ ] 创建数据库表结构
- [ ] 添加pom.xml依赖
- [ ] 创建Entity、Mapper层
- [ ] 配置CodeArts API客户端

**阶段2: Agent核心逻辑 (2-3天)**
- [ ] 实现IntentParser意图解析器
- [ ] 实现ToolExecutor工具执行器
- [ ] 实现5个Tool (Query/Create/Update/Delete/Analyze)
- [ ] 实现ResponseOptimizer响应优化器
- [ ] 实现ContextManager上下文管理器
- [ ] 实现WorkItemAgent主循环

**阶段3: Service和Controller (1天)**
- [ ] 实现AgentService业务逻辑
- [ ] 实现AgentController REST API
- [ ] 单元测试

**阶段4: 前端界面 (2天)**
- [ ] 创建AgentChat主界面
- [ ] 实现MessageBubble组件
- [ ] 实现WorkItemCard组件
- [ ] 实现SuggestionChip组件
- [ ] 实现FeedbackStar组件
- [ ] API对接

**阶段5: 测试和优化 (1-2天)**
- [ ] 集成测试
- [ ] 性能优化
- [ ] 用户体验优化
- [ ] 文档完善

### 8.2 关键技术点

1. **意图识别准确率**: 初期使用规则匹配,后期可引入NLP模型提升准确率
2. **上下文管理**: 维护对话历史,支持多轮对话
3. **错误处理**: 友好的错误提示和恢复建议
4. **反馈机制**: 收集用户反馈,持续优化响应质量
5. **API限流**: 控制CodeArts API调用频率,避免超限

---

## 9. 扩展方向

### 9.1 短期优化
- 支持更多自然语言表达方式
- 增加工作项批量操作
- 支持自定义字段查询
- 增加语音输入支持

### 9.2 长期规划
- 集成LLM大语言模型提升理解能力
- 支持多Agent协作 (查询Agent + 分析Agent + 推荐Agent)
- 智能推荐相关工作项
- 自动生成工作报告
- 支持其他项目管理平台 (Jira、ONES等)

---

## 10. 风险评估

| 风险项 | 影响 | 概率 | 应对措施 |
|--------|------|------|----------|
| CodeArts API限流 | 高 | 中 | 实现缓存机制,控制调用频率 |
| 意图识别不准 | 中 | 高 | 提供手动修正,收集反馈优化 |
| 上下文丢失 | 中 | 低 | 定期清理,限制上下文大小 |
| 并发性能问题 | 中 | 低 | 异步处理,连接池优化 |

---

## 11. 总结

本设计文档详细描述了一个基于AgentScope Java框架的智能Agent模块,采用"持续反馈-动作-优化"模式实现与华为云CodeArts Req的自然语言交互。核心特点包括:

1. **ReAct模式**: Feedback(反馈解析) → Action(动作执行) → Optimize(优化策略)的循环
2. **模块化设计**: 意图解析、工具执行、响应优化分离,易于扩展
3. **持续学习**: 通过用户反馈不断优化响应质量
4. **友好交互**: 自然语言输入,结构化输出,建议操作引导

该设计遵循SDD模式,先完成详细技术设计,再进行编码实现,确保开发过程有序可控。
