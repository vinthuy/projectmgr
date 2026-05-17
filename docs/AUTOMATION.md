# 自动化设计文档

## 1. 概述

### 1.1 目的
设计类似Jira Automation的自动化引擎，支持：
- 触发器 (Triggers) - 规则触发条件
- 条件 (Conditions) - 规则执行条件判断
- 动作 (Actions) - 规则执行的动作
- Smart Values - 智能值模板
- 规则分支 - 对相关工作项执行动作
- 审计日志 - 规则执行记录

### 1.2 设计目标
```
自动化规则结构:
┌─────────────────────────────────────────────────────────────────┐
│  Rule: 自动分配高优先级Bug                          │
├─────────────────────────────────────────────────────────────────┤
│  Trigger: issuecreated                                   │
│           (工作项创建时触发)                          │
├─────────────────────────────────────────────────────────────────┤
│  Conditions:                                         │
│    • Issue Type = Bug                                 │
│    AND Priority IN (Highest, High)                 │
├─────────────────────────────────────────────────────────────────┤
│  Actions:                                           │
│    1. Assign = (currentUser)                        │
│    2. Set Field: priority = High                   │
│    3. Transition to "In Progress"               │
│    4. Comment: "Auto-assigned to {{actor}}"        │
└─────────────────────────────────────────────────────────────────┘

Smart Values示例:
  {{issue.key}}          → PROJ-123
  {{issue.summary}}      → Fix login bug
  {{actor}}             → john.doe
  {{now.plusDays(5)}}   → 2024-01-06
  {{customField.priority}} → High
```

---

## 2. 数据模型

### 2.1 规则表

```sql
-- =============================================
-- Automation Tables
-- =============================================

-- 自动化规则
DROP TABLE IF EXISTS automation_rule CASCADE;
CREATE TABLE automation_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    rule_status VARCHAR(50) DEFAULT 'ACTIVE',
    trigger_key VARCHAR(100) NOT NULL,
    trigger_config JSONB,
    execution_order INTEGER DEFAULT 0,
    allow_loop BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- 创建索引
CREATE INDEX idx_automation_tenant ON automation_rule(tenant_id);
CREATE INDEX idx_automation_project ON automation_rule(project_id);
CREATE INDEX idx_automation_status ON automation_rule(rule_status);

-- 触发器类型:
COMMENT ON COLUMN automation_rule.trigger_key IS 
'issue.created|issue.updated|issue.transitioned|issue.commented|field.changed|scheduled|webhook';
```

### 2.2 条件表

```sql
-- 自动化条件
DROP TABLE IF EXISTS automation_condition CASCADE;
CREATE TABLE automation_condition (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES automation_rule(id),
    condition_key VARCHAR(100) NOT NULL,
    condition_config JSONB NOT NULL,
    logical_operator VARCHAR(10) DEFAULT 'AND',
    condition_sequence INTEGER DEFAULT 0
);

CREATE INDEX idx_automation_condition_rule ON automation_condition(rule_id);
```

### 2.3 动作表

```sql
-- 自动化动作
DROP TABLE IF EXISTS automation_action CASCADE;
CREATE TABLE automation_action (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES automation_rule(id),
    action_key VARCHAR(100) NOT NULL,
    action_config JSONB NOT NULL,
    action_sequence INTEGER DEFAULT 0
);

CREATE INDEX idx_automation_action_rule ON automation_action(rule_id);
```

### 2.4 分支表

```sql
-- 自动化分支（对相关工作项执行动作）
DROP TABLE IF EXISTS automation_branch CASCADE;
CREATE TABLE automation_branch (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL REFERENCES automation_rule(id),
    branch_key VARCHAR(100) NOT NULL,
    branch_config JSONB,
    UNIQUE(rule_id, branch_key)
);

-- 分支类型:
-- 1. linked_issues: 链接的工作项
--    config: {linkType: "blocks", statusFilter: "Open"}
-- 2. subtasks: 子任务
--    config: {}
-- 3. epic_issues: Epic下的所有Story
--    config: {}
-- 4. parent_issue: 父工作项
--    config: {}
```

### 2.5 审计日志表

```sql
-- 自动化审计日志
DROP TABLE IF EXISTS automation_audit_log CASCADE;
CREATE TABLE automation_audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    work_item_id BIGINT,
    trigger_event JSONB,
    conditions_result JSONB,
    actions_executed JSONB,
    execution_result VARCHAR(20) NOT NULL,
    error_message TEXT,
    execution_time_ms BIGINT,
    executed_by BIGINT,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_automation_audit_rule ON automation_audit_log(rule_id, executed_at DESC);
CREATE INDEX idx_automation_audit_item ON automation_audit_log(work_item_id, executed_at DESC);
```

### 2.6 模板表

```sql
-- 自动化模板（预置规则）
DROP TABLE IF EXISTS automation_template CASCADE;
CREATE TABLE automation_template (
    id BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(50) NOT NULL UNIQUE,
    template_name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    template_data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 3. API设计

### 3.1 规则API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/automations | 创建规则 |
| GET | /api/automations | 规则列表 |
| GET | /api/automations/{id} | 规则详情 |
| PUT | /api/automations/{id} | 更新规则 |
| DELETE | /api/automations/{id} | 删除规则 |
| POST | /api/automations/{id}/enable | 启用规则 |
| POST | /api/automations/{id}/disable | 禁用规则 |
| POST | /api/automations/{id}/run | 测试运行规则 |
| GET | /api/automations/{id}/audit | 审计日志 |

### 3.2 测试API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/automations/test | 测试规则 |
| GET | /api/automations/templates | 规则模板 |

### 3.3 请求示例

**创建规则:**
```json
POST /api/automations
Request:
{
  "name": "自动分配高优先级Bug",
  "description": "当创建高优先级Bug时自动分配给当前用户",
  "projectId": 1,
  "triggerKey": "issue.created",
  "triggerConfig": {},
  "conditions": [
    {
      "conditionKey": "field_equals",
      "conditionConfig": {"fieldKey": "issueType", "value": "bug"},
      "logicalOperator": "AND"
    },
    {
      "conditionKey": "field_equals",
      "conditionConfig": {"fieldKey": "priority", "value": "High"},
      "logicalOperator": "AND"
    }
  ],
  "actions": [
    {
      "actionKey": "assign",
      "actionConfig": {"userKey": "{{actor}}"}
    },
    {
      "actionKey": "transition",
      "actionConfig": {"workflowKey": "default", "transitionKey": "start"}
    }
  ]
}
```

---

## 4. Smart Values

### 4.1 支持的智能值

```java
public class SmartValueResolver {
    
    /**
     * 解析Smart Value模板
     * 例如: "Hello {{issue.key}}, assigned to {{actor}}"
     */
    public String resolve(String template, WorkItem workItem, User actor) {
        String result = template;
        
        // 工作项相关
        result = resolveIssueValues(result, workItem);
        
        // 用户相关
        result = resolveUserValues(result, actor);
        
        // 时间相关
        result = resolveTimeValues(result);
        
        // 自定义字段
        result = resolveCustomFieldValues(result, workItem);
        
        return result;
    }
    
    private String resolveIssueValues(String template, WorkItem issue) {
        Map<String, String> replacements = new HashMap<>();
        
        replacements.put("{{issue.key}}", issue.getItemKey());
        replacements.put("{{issue.summary}}", issue.getSummary());
        replacements.put("{{issue.description}}", issue.getDescription() != null ? 
            issue.getDescription() : "");
        replacements.put("{{issue.status}}", issue.getStatus());
        replacements.put("{{issue.priority}}", issue.getPriority());
        replacements.put("{{issue.assignee}}", issue.getAssigneeUserId() != null ? 
            userService.getById(issue.getAssigneeUserId()).getDisplayName() : "Unassigned");
        replacements.put("{{issue.reporter}}", userService.getById(issue.getReporterUserId()).getDisplayName());
        replacements.put("{{issue.created}}", issue.getCreatedAt().toString());
        replacements.put("{{issue.updated}}", issue.getUpdatedAt().toString());
        
        return replaceValues(template, replacements);
    }
    
    private String resolveTimeValues(String template) {
        LocalDateTime now = LocalDateTime.now();
        
        Map<String, String> replacements = new HashMap<>();
        
        replacements.put("{{now}}", now.toString());
        replacements.put("{{now.plusDays(1)}}", now.plusDays(1).toString());
        replacements.put("{{now.plusDays(5)}}", now.plusDays(5).toString());
        replacements.put("{{now.plusWeeks(1)}}", now.plusWeeks(1).toString());
        replacements.put("{{now.plusMonths(1)}}", now.plusMonths(1).toString());
        
        replacements.put("{{today}}", now.toLocalDate().toString());
        replacements.put("{{tomorrow}}", now.plusDays(1).toLocalDate().toString());
        
        return replaceValues(template, replacements);
    }
}
```

### 4.2 Smart Values参考表

| Smart Value | 说明 | 示例输出 |
|-------------|------|----------|
| {{issue.key}} | 工作项Key | PROJ-123 |
| {{issue.summary}} | 标题 | Fix bug |
| {{issue.description}} | 描述 | ... |
| {{issue.status}} | 状态 | Open |
| {{issue.priority}} | 优先级 | High |
| {{issue.assignee}} | 负责人 | John |
| {{issue.reporter}} | 报告人 | Jane |
| {{issue.created}} | 创建时间 | 2024-01-01T10:00:00 |
| {{issue.updated}} | 更新时间 | 2024-01-02T10:00:00 |
| {{actor}} | 触发用户 | john |
| {{currentUser}} | 当前用户 | john |
| {{now}} | 当前时间 | 2024-01-01T10:00:00 |
| {{now.plusDays(n)}} | +n天 | ... |
| {{now.plusWeeks(n)}} | +n周 | ... |
| {{today}} | 今天 | 2024-01-01 |
| {{customField.fieldKey}} | 自定义字段 | ... |

---

## 5. 触发器/条件/动作

### 5.1 触发器类型

| key | name_zh | 说明 | config |
|-----|---------|------|--------|
| issue.created | 工作项创建 | {} |
| issue.updated | 工作项更新 | {"fields": ["status", "priority"]} |
| issue.transitioned | 状态转换 | {"fromStatus": "Open", "toStatus": "In Progress"} |
| issue.commented | 添加评论 | {} |
| field.changed | 字段变更 | {"fieldKey": "priority"} |
| scheduled | 定时触发 | {"cron": "0 0 9 * * MON-FRI"} |
| webhook | Webhook触发 | {"webhookKey": "xxx"} |

### 5.2 条件类型

| key | name_zh | 说明 | config |
|-----|---------|------|--------|
| field_equals | 字段等于 | {fieldKey, value} |
| field_not_equals | 字段不等于 | {fieldKey, value} |
| field_contains | 字段包含 | {fieldKey, value} |
| field_empty | 字段为空 | {fieldKey} |
| field_not_empty | 字段不为空 | {fieldKey} |
| issue_type | 问题类型是 | {issueTypeKeys: []} |
| project | 项目是 | {projectKey} |
| priority_in | 优先级在 | {priorities: []} |
| status_in | 状态在 | {statuses: []} |
| assignee_is | 负责人是 | {userKey} |
| reporter_is | 报告人是 | {userKey} |
| linked_issue | 链接工作项 | {linkType, status} |

### 5.3 动作类型

| key | name_zh | 说明 | config |
|-----|---------|------|--------|
| transition | 状态转换 | {workflowKey, transitionKey} |
| assign | 分配 | {userKey} |
| set_field | 设置字段 | {fieldKey, value} |
| add_labels | 添加标签 | {labels: []} |
| remove_labels | 移除标签 | {labels: []} |
| add_comment | 添加评论 | {content, internal} |
| notify | 发送通知 | {recipients: [], template} |
| create_issue | 创建工作项 | {projectKey, issueType, summary} |
| clone_issue | 克隆工作项 | {} |
| web request | 发送请求 | {url, method, headers, body} |

---

## 6. 核心服务

### 6.1 规则引擎

```java
public class AutomationEngine {
    
    @Autowired
    private AutomationRuleMapper ruleMapper;
    
    @Autowired
    private WorkItemService workItemService;
    
    /**
     * 触发规则执行
     */
    public void trigger(AutomationTriggerEvent event) {
        List<AutomationRule> rules = ruleMapper.findByTrigger(event.getTriggerKey());
        
        for (AutomationRule rule : rules) {
            if (rule.getRuleStatus().equals("DISABLED")) continue;
            
            if (matchesProject(rule, event) && matchesConditions(rule, event)) {
                executeRule(rule, event);
            }
        }
    }
    
    /**
     * 执行规则
     */
    private void executeRule(AutomationRule rule, AutomationTriggerEvent event) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行动作
            for (AutomationAction action : rule.getActions()) {
                executeAction(action, event.getWorkItem(), event.getActor());
            }
            
            // 记录审计日志
            logAudit(rule, event, "SUCCESS", 
                System.currentTimeMillis() - startTime);
                
        } catch (Exception e) {
            logAudit(rule, event, "FAILED", 
                System.currentTimeMillis() - startTime, e.getMessage());
        }
    }
    
    /**
     * 执行动作
     */
    private void executeAction(AutomationAction action, WorkItem workItem, User actor) {
        switch (action.getActionKey()) {
            case "transition":
                executeTransition(action.getActionConfig(), workItem);
                break;
            case "assign":
                executeAssign(action.getActionConfig(), workItem, actor);
                break;
            case "set_field":
                executeSetField(action.getActionConfig(), workItem);
                break;
            case "add_comment":
                executeAddComment(action.getActionConfig(), workItem, actor);
                break;
            case "web_request":
                executeWebRequest(action.getActionConfig(), workItem);
                break;
            case "create_issue":
                executeCreateIssue(action.getActionConfig(), workItem, actor);
                break;
            default:
                log.warn("Unknown action: {}", action.getActionKey());
        }
    }
}
```

### 6.2 条件评估

```java
public class AutomationConditionEvaluator {
    
    public boolean evaluate(List<AutomationCondition> conditions, 
                       WorkItem workItem, User actor) {
        boolean result = true;
        
        for (AutomationCondition condition : conditions) {
            boolean condResult = evaluateCondition(condition, workItem, actor);
            
            if (condition.getLogicalOperator().equals("AND")) {
                result = result && condResult;
            } else {
                result = result || condResult;
            }
        }
        
        return result;
    }
    
    private boolean evaluateCondition(AutomationCondition condition,
                                   WorkItem workItem, User actor) {
        JSONObject config = condition.getConditionConfig();
        
        switch (condition.getConditionKey()) {
            case "field_equals":
                return workItem.getField(config.getString("fieldKey"))
                    .equals(config.get("value"));
                    
            case "field_contains":
                String value = workItem.getField(config.getString("fieldKey"));
                return value != null && value.contains(config.getString("value"));
                
            case "field_empty":
                return workItem.getField(config.getString("fieldKey")) == null;
                
            case "issue_type":
                JSONArray types = config.getJSONArray("issueTypeKeys");
                return types.contains(workItem.getIssueTypeId());
                
            case "priority_in":
                JSONArray priorities = config.getJSONArray("priorities");
                return priorities.contains(workItem.getPriority());
                
            default:
                return true;
        }
    }
}
```

### 6.3 Webhook端点

```java
@RestController
@RequestMapping("/api/webhooks/automation")
public class AutomationWebhookController {
    
    @PostMapping
    public Result<Void> handleWebhook(
            @RequestBody WebhookPayload payload,
            @RequestHeader("X-Webhook-Secret") String secret) {
        
        // 验证密钥
        if (!webhookSecretService.validate(secret, payload.getWebhookKey())) {
            return Result.error(401, "Invalid secret");
        }
        
        // 触发规则
        AutomationTriggerEvent event = payload.toTriggerEvent();
        automationEngine.trigger(event);
        
        return Result.success();
    }
}
```

---

## 7. 预置模板

### 7.1 Jira预置模板

```json
{
  "templates": [
    {
      "template_key": "auto_assign_bug",
      "template_name": "自动分配Bug",
      "category": "assignment",
      "template_data": {
        "name": "Auto-assign new bugs",
        "triggerKey": "issue.created",
        "conditions": [
          {"conditionKey": "issue_type", "conditionConfig": {"issueTypeKeys": ["bug"]}}
        ],
        "actions": [
          {"actionKey": "assign", "actionConfig": {"userKey": "{{actor}}"}},
          {"actionKey": "add_comment", "actionConfig": {"content": "Assigned to {{actor}}"}}
        ]
      }
    },
    {
      "template_key": "close_resolved",
      "template_name": "3天后自动关闭已解决",
      "category": "time_based",
      "template_data": {...}
    },
    {
      "template_key": "slack_notify",
      "template_name": "高优先级通知Slack",
      "category": "notification",
      "template_data": {...}
    }
  ]
}
```

---

## 8. 验收标准

### 8.1 触发器验收
- [ ] 支持issue.created触发
- [ ] 支持issue.updated触发
- [ ] 支持field.changed触发
- [ ] 支持scheduled触发
- [ ] 支持webhook触发

### 8.2 条件验收
- [ ] field_equals评估正确
- [ ] 支持AND/OR逻辑组合
- [ ] 多条件正确短路评估

### 8.3 动作验收
- [ ] transition正确执行
- [ ] assign正确设置负责人
- [ ] set_field正确设置字段
- [ ] add_comment正确添加评论
- [ ] Smart Values正确解析

### 8.4 审计验收
- [ ] 记录每次执行结果
- [ ] 记录执行时间
- [ ] 记录错误信息

### 8.5 场景测试

**场景1: 高优先级Bug自动分配**
```
Trigger: issue.created
Conditions:
  - issue_type = bug
  - priority IN (Highest, High)
Actions:
  - assign = {{actor}}
  - transition = start
```

**场景2: 状态变更通知**
```
Trigger: issue.transitioned
Conditions:
  - status = Done
Actions:
  - add_comment = "Task completed by {{actor}}"
  - notify = {recipients: ["reporter"], template: "completed"}
```

**场景3: 定时提醒**
```
Trigger: scheduled
Config: {"cron": "0 0 9 * * MON-FRI"}
Conditions:
  - status = In Progress
  - dueDate = today
Actions:
  - notify = {recipients: ["assignee"], template: "overdue"}
```