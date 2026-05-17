# 工作流增强设计文档

## 1. 概述

### 1.1 目的
增强工作流引擎，支持：
- 条件 (Conditions) - 控制转换是否可执行
- 验证器 (Validators) - 验证转换输入
- 后动作 (Post Functions) - 转换后的额外处理
- 触发器 (Triggers) - 外部事件触发转换

### 1.2 设计目标
```
工作流转换器:
┌─────────────────────────────────────────────────────────────────┐
│           Transition: TODO → IN_PROGRESS              │
├─────────────────────────────────────────────────────────────────┤
│  Conditions (条件):                                  │
│    • 用户在项目成员中                                │
│    • assignee字段已设置                             │
├─────────────────────────────────────────────────────────────────┤
│  Validators (验证器):                                 │
│    • 摘要不能为空                                   │
│    • 优先级必须有效值                                │
├─────────────────────────────────────────────────────────────────┤
│  Post Functions (后动作):                           │
│    • 更新状态为IN_PROGRESS                          │
│    • 设置updatedAt = now()                         │
│    • 添加历史记录                                  │
│    • 发送通知给assignee                             │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Triggers (触发器)                       │
├─────────────────────────────────────────────────────────────────┤
│  • Bitbucket: PR Merged → transition to "In Review"    │
│  • GitHub: Issue Closed → transition to "Done"        │
│  • CI/CD: Build Failed → transition to "Open"         │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 数据模型

### 2.1 条件表

```sql
-- =============================================
-- Workflow Conditions
-- =============================================

-- 工作流条件定义
DROP TABLE IF EXISTS workflow_condition CASCADE;
CREATE TABLE workflow_condition (
    id BIGSERIAL PRIMARY KEY,
    transition_id BIGINT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    condition_config JSONB NOT NULL,
    logical_operator VARCHAR(10) DEFAULT 'AND',
    condition_sequence INTEGER DEFAULT 0,
    UNIQUE(transition_id, condition_key)
);

-- 条件类型详解:
-- 1. user_in_group: 用户在组中
--    config: {"groupKey": "developers"}
-- 2. user_has_permission: 用户有权限
--    config: {"permissionKey": "Edit Issues"}
-- 3. field_is_set: 字段已设置
--    config: {"fieldKey": "assignee"}
-- 4. field_equals: 字段值等于
--    config: {"fieldKey": "priority", "value": "High"}
-- 5. previous_status: 上一个状态是
--    config: {"statusKey": "IN_PROGRESS"}
-- 6. linked_issue_condition: 链接的工作项条件
--    config: {"linkType": "blocks", "status": "Open"}
-- 7. subtask_all_done: 子任务全部完成
--    config: {}
```

### 2.2 验证器表

```sql
-- =============================================
-- Workflow Validators
-- =============================================

-- 工作流验证器定义
DROP TABLE IF EXISTS workflow_validator CASCADE;
CREATE TABLE workflow_validator (
    id BIGSERIAL PRIMARY KEY,
    transition_id BIGINT NOT NULL,
    validator_key VARCHAR(100) NOT NULL,
    validator_config JSONB,
    error_message VARCHAR(500),
    validator_sequence INTEGER DEFAULT 0,
    UNIQUE(transition_id, validator_key)
);

-- 验证器类型:
-- 1. required_field: 必填字段验证
--    config: {"fieldKeys": ["summary", "assignee"]}
-- 2. field_validation: 字段值验证
--    config: {"fieldKey": "priority", "pattern": "^[A-Z]$"}
-- 3. permissions_check: 权限检查
--    config: {"permissionKey": "Edit Issues"}
-- 4. comment_author: 评论作者验证
--    config: {"commentRequired": true}
-- 5. linked_issue_status: 链接工作项状态验证
--    config: {"linkType": "blocks", "requiredStatus": "Done"}
```

### 2.3 后动作表

```sql
-- =============================================
-- Workflow Post Functions
-- =============================================

-- 工作流后动作定义
DROP TABLE IF EXISTS workflow_post_function CASCADE;
CREATE TABLE workflow_post_function (
    id BIGSERIAL PRIMARY KEY,
    transition_id BIGINT NOT NULL,
    function_key VARCHAR(100) NOT NULL,
    function_config JSONB,
    function_sequence INTEGER DEFAULT 0,
    UNIQUE(transition_id, function_key)
);

-- 后动作类型:
-- 1. update_field: 更新字段
--    config: {"fieldKey": "resolution", "value": "Fixed"}
-- 2. clear_field: 清除字段值
--    config: {"fieldKeys": ["timeEstimate", "timeSpent"]}
-- 3. set_field_current_user: 设置为当前用户
--    config: {"fieldKey": "assignee", "use": "currentUser"}
-- 4. generate_change_history: 生成变更历史
--    config: {}
-- 5. create_comment: 创建评论
--    config: {"template": "Status changed to {{toStatus}}", "internal": false}
-- 6. trigger_notification: 触发通知
--    config: {"eventType": "status_changed", "recipients": ["assignee", "reporter"]}
-- 7. fire_event: 触发事件
--    config: {"eventKey": "issue.transitioned"}
-- 8. run_automation: 运行自动化规则
--    config: {"automationIds": [1, 2, 3]}
```

### 2.4 触发器表

```sql
-- =============================================
-- Workflow Triggers
-- =============================================

-- 工作流触发器
DROP TABLE IF EXISTS workflow_trigger CASCADE;
CREATE TABLE workflow_trigger (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config JSONB NOT NULL,
    target_step_id BIGINT,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 触发器类型:
-- 1. code_drop: 代码推送
--    config: {"repoKey": "backend", "branchPattern": "main", "commitMessagePattern": "closes #{{issue.key}}"}
-- 2. pull_request: PR创建/合并
--    config: {"repoKey": "backend", "action": "MERGED", "sourceBranchPattern": "feature/*"}
-- 3. branch_created: 分支创建
--    config: {"repoKey": "backend", "branchPattern": "{{issue.key}}-*"}
-- 4. build_completed: 构建完成
--    config: {"buildKey": "backend-ci", "buildStatus": "SUCCESS"}
-- 5. webhook: 外部Webhook
--    config: {"webhookUrl": "https://external.com/trigger", "secret": "xxx"}
```

### 2.5 全局转换

```sql
-- =============================================
-- Global Transitions
-- =============================================

-- 全局转换配置
DROP TABLE IF EXISTS workflow_global_transition CASCADE;
CREATE TABLE workflow_global_transition (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    source_step_id BIGINT,
    target_step_id BIGINT NOT NULL,
    trigger_name VARCHAR(200),
    trigger_key VARCHAR(50),
    UNIQUE(workflow_id, source_step_id, target_step_id)
);

CREATE INDEX idx_workflow_transition ON workflow_transition(workflow_id);
CREATE INDEX idx_workflow_condition_trans ON workflow_condition(transition_id);
CREATE INDEX idx_workflow_validator_trans ON workflow_validator(transition_id);
CREATE INDEX idx_workflow_postfunc_trans ON workflow_post_function(transition_id);
```

---

## 3. API设计

### 3.1 条件API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/workflows/{id}/transitions/{tid}/conditions | 添加条件 |
| GET | /api/workflows/{id}/transitions/{tid}/conditions | 获取条件列表 |
| PUT | /api/workflows/{id}/transitions/{tid}/conditions/{cid} | 更新条件 |
| DELETE | /api/workflows/{id}/transitions/{tid}/conditions/{cid} | 删除条件 |

### 3.2 验证器API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/workflows/{id}/transitions/{tid}/validators | 添加验证器 |
| GET | /api/workflows/{id}/transitions/{tid}/validators | 获取验证器列表 |
| DELETE | /api/workflows/{id}/transitions/{tid}/validators/{vid} | 删除验证器 |

### 3.3 后动作API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/workflows/{id}/transitions/{tid}/post-functions | 添加后动作 |
| GET | /api/workflows/{id}/transitions/{tid}/post-functions | 获取后动作列表 |
| PUT | /api/workflows/{id}/transitions/{tid}/post-functions/{pid} | 更新后动作 |
| DELETE | /api/workflows/{id}/transitions/{tid}/post-functions/{pid} | 删除后动作 |

### 3.4 触发器API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/workflows/{id}/triggers | 添加触发器 |
| GET | /api/workflows/{id}/triggers | 获取触发器列表 |
| POST | /api/workflows/{id}/triggers/{tid}/enable | 启用触发器 |
| POST | /api/workflows/{id}/triggers/{tid}/disable | 禁用触发器 |
| DELETE | /api/workflows/{id}/triggers/{tid} | 删除触发器 |

### 3.5 请求示例

**添加条件:**
```json
POST /api/workflows/1/transitions/5/conditions
Request:
{
  "conditionKey": "user_has_permission",
  "conditionConfig": {
    "permissionKey": "Transition Issues"
  },
  "logicalOperator": "AND"
}

**添加验证器:**
```json
POST /api/workflows/1/transitions/5/validators
Request:
{
  "validatorKey": "required_field",
  "validatorConfig": {
    "fieldKeys": ["summary"]
  },
  "errorMessage": "摘要不能为空"
}
```

**添加后动作:**
```json
POST /api/workflows/1/transitions/5/post-functions
Request:
{
  "functionKey": "set_field_current_user",
  "functionConfig": {
    "fieldKey": "assignee",
    "use": "currentUser"
  }
}
```

**添加触发器:**
```json
POST /api/workflows/1/triggers
Request:
{
  "triggerType": "code_drop",
  "triggerConfig": {
    "repoKey": "backend",
    "commitMessagePattern": "closes #{{issue.key}}"
  },
  "targetStepId": 3
}
```

---

## 4. 核心服务

### 4.1 条件评估

```java
public class WorkflowConditionEvaluator {
    
    public boolean evaluate(Transition transition, WorkItem workItem, User user, Map<String, Object> params) {
        List<WorkflowCondition> conditions = workflowConditionMapper.findByTransitionId(transition.getId());
        
        boolean result = true;
        for (WorkflowCondition condition : conditions) {
            boolean condResult = evaluateCondition(condition, workItem, user, params);
            
            if (condition.getLogicalOperator().equals("AND")) {
                result = result && condResult;
            } else {
                result = result || condResult;
            }
        }
        return result;
    }
    
    private boolean evaluateCondition(WorkflowCondition condition, WorkItem workItem, 
                             User user, Map<String, Object> params) {
        switch (condition.getConditionKey()) {
            case "user_in_group":
                return evaluateUserInGroup(condition.getConditionConfig(), user);
            case "user_has_permission":
                return evaluateUserPermission(condition.getConditionConfig(), user);
            case "field_is_set":
                return evaluateFieldIsSet(condition.getConditionConfig(), workItem);
            case "field_equals":
                return evaluateFieldEquals(condition.getConditionConfig(), workItem);
            case "previous_status":
                return evaluatePreviousStatus(condition.getConditionConfig(), workItem);
            default:
                return true;
        }
    }
    
    // 示例: user_has_permission
    private boolean evaluateUserPermission(JSONObject config, User user) {
        String permissionKey = config.getString("permissionKey");
        return permissionService.hasPermission(user, permissionKey);
    }
}
```

### 4.2 验证器

```java
public class WorkflowValidator {
    
    public List<ValidationError> validate(Transition transition, 
                                       WorkItem workItem, 
                                       Map<String, Object> params) {
        List<ValidationError> errors = new ArrayList<>();
        
        List<WorkflowValidator> validators = 
            workflowValidatorMapper.findByTransitionId(transition.getId());
        
        for (WorkflowValidator validator : validators) {
            List<ValidationError> validatorErrors = validateValidator(
                validator, workItem, params);
            errors.addAll(validatorErrors);
        }
        
        return errors;
    }
    
    private List<ValidationError> validateValidator(WorkflowValidator validator,
                                                WorkItem workItem,
                                                Map<String, Object> params) {
        switch (validator.getValidatorKey()) {
            case "required_field":
                return validateRequiredField(validator.getValidatorConfig(), workItem);
            case "field_validation":
                return validateFieldValue(validator.getValidatorConfig(), workItem);
            default:
                return Collections.emptyList();
        }
    }
    
    // 示例: required_field验证
    private List<ValidationError> validateRequiredField(JSONObject config, WorkItem workItem) {
        List<ValidationError> errors = new ArrayList<>();
        JSONArray fieldKeys = config.getJSONArray("fieldKeys");
        
        for (Object fieldKey : fieldKeys) {
            Object value = workItem.getField((String) fieldKey);
            if (value == null || StringUtils.isBlank(value.toString())) {
                errors.add(new ValidationError(
                    (String) fieldKey, 
                    validator.getErrorMessage()));
            }
        }
        return errors;
    }
}
```

### 4.3 后动作执行

```java
public class WorkflowPostFunctionExecutor {
    
    public void execute(Transition transition, WorkItem workItem, 
                     User user, Map<String, Object> params) {
        List<WorkflowPostFunction> functions = 
            workflowPostFunctionMapper.findByTransitionId(transition.getId());
        
        // 按sequence顺序执行
        functions.sort(Comparator.comparing(WorkflowPostFunction::getFunctionSequence));
        
        for (WorkflowPostFunction function : functions) {
            executePostFunction(function, workItem, user, params);
        }
    }
    
    private void executePostFunction(WorkflowPostFunction function, WorkItem workItem,
                                 User user, Map<String, Object> params) {
        switch (function.getFunctionKey()) {
            case "update_field":
                executeUpdateField(function.getFunctionConfig(), workItem);
                break;
            case "clear_field":
                executeClearField(function.getFunctionConfig(), workItem);
                break;
            case "set_field_current_user":
                executeSetCurrentUser(function.getFunctionConfig(), workItem, user);
                break;
            case "generate_change_history":
                executeGenerateHistory(transition, workItem, user);
                break;
            case "create_comment":
                executeCreateComment(function.getFunctionConfig(), workItem, user);
                break;
            case "trigger_notification":
                executeTriggerNotification(function.getFunctionConfig(), workItem, user);
                break;
            default:
                log.warn("Unknown post function: {}", function.getFunctionKey());
        }
    }
}
```

### 4.4 触发器处理

```java
public class WorkflowTriggerHandler {
    
    /**
     * 处理外部事件触发
     */
    public void handleTriggerEvent(TriggerEvent event) {
        List<WorkflowTrigger> triggers = workflowTriggerMapper.findByType(event.getEventType());
        
        for (WorkflowTrigger trigger : triggers) {
            if (trigger.isEnabled() && matchesConfig(trigger, event)) {
                executeTrigger(trigger, event);
            }
        }
    }
    
    /**
     * Webhook端点
     */
    @PostMapping("/api/webhooks/workflow")
    public Result<Void> handleWebhook(@RequestBody WebhookPayload payload,
                                     @RequestHeader("X-Webhook-Signature") String signature) {
        // 验证签名
        if (!verifySignature(payload, signature)) {
            return Result.error(401, "Invalid signature");
        }
        
        // 触发工作流
        handleTriggerEvent(payload.toTriggerEvent());
        return Result.success();
    }
}
```

---

## 5. 预置条件/验证器/后动作

### 5.1 预置条件

| key | name_en | name_zh | config |
|-----|---------|---------|---------|--------|
| user_in_group | User is in Group | 用户在组中 | groupKey |
| user_has_permission | User has Permission | 用户有权限 | permissionKey |
| field_is_set | Field is Set | 字段已设置 | fieldKey |
| field_equals | Field Equals | 字段值等于 | fieldKey, value |
| field_not_equals | Field Not Equals | 字段值不等于 | fieldKey, value |
| previous_status | Previous Status | 上一个状态是 | statusKey |
| linked_issue_condition | Linked Issue Condition | 链接���作���条件 | linkType, status |
| subtask_all_done | All Sub-tasks Done | 子任务全部完成 | - |

### 5.2 预置验证器

| key | name_en | name_zh | config |
|-----|---------|---------|---------|--------|
| required_field | Required Field | 必填字段 | fieldKeys[] |
| field_validation | Field Validation | 字段验证 | fieldKey, pattern |
| permissions_check | Permissions Check | 权限检查 | permissionKey |
| comment_exists | Comment Exists | 存在评论 | - |
| linked_issue_resolved | Linked Issue Resolved | 链接工作项已解决 | linkType |

### 5.3 预置后动作

| key | name_en | name_zh |
|-----|---------|---------|--------|
| update_field | Update Field | 更新字段 |
| clear_field | Clear Field | 清除字段 |
| set_field_current_user | Set Field to Current User | 设置为当前用户 |
| generate_change_history | Generate Change History | 生成变更历史 |
| create_comment | Create Comment | 创建评论 |
| trigger_notification | Trigger Notification | 触发通知 |
| fire_event | Fire Event | 触发事件 |

---

## 6. 验收标准

### 6.1 条件验收
- [ ] 支持添加条件到转换
- [ ] 支持AND/OR逻辑组合
- [ ] 评估正确返回true/false
- [ ] 不满足条件时阻止转换

### 6.2 验证器验收
- [ ] 支持添加验证器到转换
- [ ] 验证失败时显示错误消息
- [ ] 阻止无效转换

### 6.3 后动作验收
- [ ] 支持添加后动作
- [ ] 按顺序执行
- [ ] 正确更新字段/创建评论/生成历史

### 6.4 触发器验收
- [ ] 支持外部Webhook触发
- [ ] 支持代码事件触发
- [ ] 正确执行状态转换

### 6.5 场景测试

**场景1: 只有assignee才能将任务标记为完成**
```
Transition: IN_PROGRESS → DONE
Conditions:
  - user_has_permission: {permissionKey: "Transition Issues"}
  - field_is_set: {fieldKey: "assignee"}
```

**场景2: 转换时必须填写解决方案**
```
Transition: IN_PROGRESS → DONE
Validators:
  - required_field: {fieldKeys: ["resolution"]}
```

**场景3: 转换完成后自动清除预估时间**
```
Transition: IN_PROGRESS → DONE
Post Functions:
  - update_field: {fieldKey: "resolution", "value": "Done"}
  - clear_field: {fieldKeys: ["timeEstimate"]}
  - create_comment: {template: "Status changed to Done"}
```

**场景4: Git提交信息自动触发**
```
Trigger: code_drop
Config: {commitMessagePattern: "closes #{{issue.key}}"}
Target: DONE
```