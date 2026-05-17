# Jira 特性对比分析文档

## 1. Jira 核心特性概览

| 特性类别 | Jira 特性 | 本系统现状 | 差距分析 | 实现优先级 |
|---------|----------|----------|----------|----------|
| **工作项管理** | | | | | |
| | 多类型支持 | ✓ | task/bug/story/epic/subtask | 已实现 | P0 |
| | 级联关系 | △ | 父子关系 | 需支持epic→story→subtask层级 | P1 |
| | 链接关系 | ✗ | 无 | 需实现blocks/relates等关系 | P1 |
| | 草稿模式 | ✗ | 无 | 需实现未确认创建 | P2 |
| **字段系统** | | | | | |
| | 自定义字段 | ✓ | 支持 | 已实现 | P0 |
| | 字段上下文 | ✗ | 全局配置 | 需按项目/类型隔离 | P1 |
| | 字段配置方案 | ✗ | 无 | 需实现field scheme | P1 |
| | 屏幕方案 | ✗ | 无 | 需实现screen scheme | P2 |
| **工作流** | | | | | |
| | 状态+转换器 | ✓ | 步骤+转换 | 已实现 | P0 |
| | 条件 | ✓ | JSON配置 | 需可视化条件构建器 | P1 |
| | 验证器 | ✓ | JSON配置 | 需预置验证器 | P1 |
| | 后动作 | ✓ | JSON配置 | 需实现post functions | P1 |
| | 触发器 | ✗ | 无 | 需支持外部触发 | P2 |
| | 全局转换 | ✗ | 无 | 需实现global transition | P2 |
| **自动化** | | | | |
| | 触发器 | ✓ | 事件触发 | 需扩展触发类型 | P1 |
| | 条件 | △ | 基础 | 需Smart Values支持 | P2 |
| | 动作 | △ | 基础 | 需扩展actions | P1 |
| | 规则分支 | ✗ | 无 | 需实现branching | P2 |
| | 审计日志 | ✗ | 无 | 需实现audit log | P2 |
| **视图/看板** | | | | | |
| | JQL查询 | ✗ | 基础条件查询 | 需实现JQL引擎 | P1 |
| | 过滤器 | △ | 基础 | 需增强 | P1 |
| | 看板 | △ | 基础board | 需WIP限制/快速操作 | P1 |
| | 敏捷面板 | ✗ | 无 | 需Scrum/Kanban | P1 |
| | 仪表盘 | ✗ | 无 | 需Dashboard | P2 |
| **权限** | | | | | |
| | 权限方案 | ✗ | 基础角色 | 需实现permission scheme | P1 |
| | 项目角色 | ✗ | 成员+角色 | 需增强 | P1 |
| | 屏幕权限 | ✗ | 无 | 需field-level permission | P2 |
| **通知** | | | | | |
| | 事件通知 | ✗ | 无 | 需事件订阅 | P2 |
| | 模板通知 | ✗ | 无 | 需通知模板 | P2 |
| **时间跟踪** | | | | | |
| | 预估时间 | ✓ | 支持 | 已实现 | P0 |
| | 记录时间 | ✓ | 支持 | 需增强日志 | P1 |
| | 剩余时间 | ✗ | 无 | 需实现 | P1 |
| **附件** | | | | | |
| | 文件上传 | ✗ | JSON存储 | 需实现对象存储 | P2 |
| | 版本控制 | ✗ | 无 | 需实现 | P2 |
| **移动端** | | | | | | |
| | 移动App | ✗ | 无 | 需独立开发 | P3 |

---

## 2. 工作项管理详细对比

### 2.1 Issue Type 层级

```
Jira:                    本系统设计:
┌─────────────────┐       ┌─────────────────┐
│     Epic        │       │     Epic       │
│   (层级2)       │       │   (层级2)      │
└────────┬────────┘       └────────┬────────┘
         │                       │
         ▼                       ▼
┌─────────────────┐       ┌─────────────────┐
│     Story        │       │     Story       │
│   (层级1)        │◄─────►│   (层级1)       │
└────────┬────────┘       └────────┬────────┘
         │                       │
    ┌────┴────┐            ┌────┴────┐
    ▼          ▼            ���          ▼
┌──────┐  ┌──────┐    ┌──────┐  ┌──────┐
│ Task │  │Bug  │    │ Task │  │ Bug │
└──────┘  └──────┘    └──────┘  └──────┘
    │          │            │          │
    ▼          ▼            ▼          ▼
┌─────────────────┐       ┌─────────────────┐
│  Sub-task  │       │  Sub-task        │
│ (层级0)  │       │  (层级0)        │
└─────────────────┘       └─────────────────┘
```

**需要增加的表:**
```sql
-- Issue Link Type
CREATE TABLE issue_link_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    link_key VARCHAR(50) NOT NULL,
    link_name VARCHAR(200) NOT NULL,
    outward_name VARCHAR(200),
    inward_name VARCHAR(200),
    is_global BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, link_key));

-- Issue Link
CREATE TABLE issue_link (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    work_item_id BIGINT NOT NULL,
    linked_item_id BIGINT NOT NULL,
    link_type_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW());
```

### 2.2 工作项草稿模式

Jira支持草稿状态：用户创建Issue时可以选择"保存为草稿"，不触发工作流和自动化。

**设计:**
```sql
-- 工作项草稿表
CREATE TABLE work_item_draft (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    summary VARCHAR(500),
    draft_data JSONB NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP);
```

---

## 3. 字段系统详细对比

### 3.1 字段上下文 (Field Context)

Jira特性：字段可以有不同的上下文配置，不同项目/类型可以使用不同配置。

**设计:**
```sql
-- 字段上下文
CREATE TABLE field_context (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    context_name VARCHAR(200) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, field_definition_id, context_name));

-- 字段上下文值配置
CREATE TABLE field_context_value (
    id BIGSERIAL PRIMARY KEY,
    context_id BIGINT NOT NULL,
    option_config JSONB,
    default_value JSONB);
```

### 3.2 屏幕方案 (Screen Scheme)

Jira使用Screen来控制字段的显示和编辑。

**设计:**
```sql
-- 屏幕方案
CREATE TABLE screen_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    UNIQUE(tenant_id, scheme_key));

-- 屏幕
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    screen_key VARCHAR(50) NOT NULL,
    screen_name VARCHAR(200) NOT NULL,
    screen_type VARCHAR(50),
    UNIQUE(tenant_id, screen_key));

-- 屏幕字段
CREATE TABLE screen_field (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    field_position INTEGER DEFAULT 0,
    is_required_display BOOLEAN DEFAULT FALSE);
```

---

## 4. 工作流详细对比

### 4.1 条件 (Conditions)

Jira条件类型:
| 条件类型 | 说明 | 本系统实现 |
|---------|------|----------|
| User Is In Group | 用户在组中 | 需实现 |
| User Has Permission | 用户有权限 | 需实现 |
| Field Is Set | 字段已设置 | 需实现 |
| Field Equals | 字段值等于 | 已实现 |
| Previous Status | 上一个状态是 | 需实现 |
| Linked Issue Condition | 链接的工作项条件 | 需实现 |
| Sub-task Blocking | 子任务阻��� | 需实现 |

### 4.2 验证器 (Validators)

Jira验证器类型:
| 验证器类型 | 说明 | 本系统实现 |
|-----------|------|----------|
| Required Field | 必填字段 | 已实现 |
| Field Validation | 字段值验证 | 需实现 |
| Permissions Check | 权限检查 | 需实现 |
| Comment Author | 评论作者 | 需实现 |

### 4.3 后动作 (Post Functions)

Jira后动作类型:
| 后动作类型 | 说明 | 本系统实现 |
|-----------|------|----------|
| Update Field | 更新字段 | 已实现 |
| Create Comment | 创建评论 | 已实现 |
| Generate Change History | 生成历史 | 需实现 |
| Trigger Email | 触发邮件通知 | 需实现 |
| Clear Field Value | 清除字段值 | 需实现 |
| Set Field Value | 设置字段值 | 已实现 |

### 4.4 触发器 (Triggers)

Jira支持的工作流触发器:
| 触发器 | 说明 | 本系统实现 |
|--------|------|----------|
| Code Drop | 代码推送 | 需集成 |
| Pull Request | PR创建 | 需集成 |
| Branch Created | 分支创建 | 需集成 |
| Build Completed | 构建完成 | 需集成 |

**设计:**
```sql
-- 工作流触发器
CREATE TABLE workflow_trigger (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    transition_id BIGINT NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config JSONB,
    is_enabled BOOLEAN DEFAULT TRUE);
```

---

## 5. 自动化详细对比

### 5.1 Jira 自动化核心概念

```
┌─────────────────────────────────────────────────────┐
│                 Automation Rule                      │
├─────────────────────────────────────────────────────┤
│  Trigger (触发器) ─────► Condition (条件) ─────► Action │
│       │                         │                    │
│  • Issue created      • Field = value         • Transition    │
│  • Field changed     • User is xxx            • Assign      │
│  • Status changed  • Issue type = xxx       • Set field   │
│  • Scheduled      • Project = xxx           • Comment    │
│  • Webhook       • More...                 • More...     │
└─────────────────────────────────────────────────────┘
```

### 5.2 Smart Values

Jira的Smart Values允许在自动化中使用模板变量:

| Smart Value | 说明 | 示例输出 |
|------------|------|----------|
| {{issue.key}} | 工作项Key | PROJ-123 |
| {{issue.summary}} | 标题 | Fix bug |
| {{issue.assignee}} | 负责人 | John |
| {{now}} | 当前时间 | 2024-01-01 |
| {{now.plusDays(5)}} | +5天 | 2024-01-06 |
| {{issue.created.plusDays(7)}} | 创建时间+7天 | ... |
| {{reporter.displayName}} | 报告人显示名 | ... |
| {{customField.X}} | 自定义字段 | ... |

**设计 - 智能值解析器:**
```java
public class SmartValueParser {
    public String parse(String template, WorkItem workItem) {
        // 支持的智能值:
        // {{issue.key}}
        // {{issue.summary}}
        // {{issue.assignee.displayName}}
        // {{customField.fieldKey}}
        // {{now.plusDays(n)}}
        // {{created.plusDays(n)}}
    }
}
```

### 5.3 规则分支 (Branching)

Jira支持对相关工作项执行动作:
- 对Epics的所有Stories执行动作
- 对Blockers的所有工作项执行动作
- 对Sub-tasks执行动作

**设计:**
```sql
-- 自动化分支规则
CREATE TABLE automation_branch (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    branch_type VARCHAR(50) NOT NULL,
    link_filter VARCHAR(100),
    UNIQUE(rule_id, branch_type));
```

### 5.4 审计日志

**设计:**
```sql
-- 自动化审计日志
CREATE TABLE automation_audit_log (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    trigger_event JSONB,
    execution_result VARCHAR(20),
    actions_executed JSONB,
    execution_time BIGINT,
    executed_by BIGINT,
    executed_at TIMESTAMP DEFAULT NOW());
```

---

## 6. JQL 查询对比

### 6.1 支持的JQL语法

| JQL语法 | Jira支持 | 本系统实现 |
|--------|--------|----------|
| project = XXX | ✓ | ✓ |
| status = XXX | ✓ | ✓ |
| assignee = currentUser() | ✓ | ✗ |
| created >= -7d | ✓ | ✗ |
| updated <= "1w" | ✓ | ✗ |
| summary ~ "text" | ✓ | ✗ |
| description ~ text | ✓ | ✗ |
| ORDER BY created DESC | ✓ | ✗ |
| ORDER BY priority ASC | ✓ | ✗ |

### 6.2 设计 - JQL解析器

```java
public class JqlParser {
    // Grammar:
    // query    → condition (AND condition)* (ORDER BY sortField (ASC|DESC)?)*
    // condition → field operator value
    // operator → = | != | > | < | >= | <= | IN | NOT IN | ~ | !~
    // value → string | number | (value, value*) | function()
    
    public Query parse(String jql);
    public List<WorkItem> execute(Query query);
}
```

---

## 7. 权限对比

### 7.1 Jira 权限方案

| 权限 | 说明 | 本系统实现 |
|------|------|----------|
| Browse Projects | 浏览项目 | 需实现 |
| Create Issues | 创建工作项 | 需实现 |
| Edit Issues | 编辑工作项 | 需实现 |
| Delete Issues | 删除工作项 | 需实现 |
| Transition Issues | 转换状态 | 需实现 |
| Manage Sprints | 管理冲刺 | 需实现 |
| Manage Boards | 管理看板 | 需实现 |
| Manage Filters | 管理过滤器 | 需实现 |

### 7.2 设计 - 权限方案

```sql
-- Permission Scheme
CREATE TABLE permission_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    UNIQUE(tenant_id, scheme_key));

-- Permission
CREATE TABLE permission (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    description TEXT);

-- Permission Grant
CREATE TABLE permission_grant (
    id BIGSERIAL PRIMARY KEY,
    permission_id BIGINT NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    grant_key VARCHAR(100));
```

---

## 8. 看板/敏捷详细对比

### 8.1 Jira 看板特性

| 特性 | Jira | 本系统 |
|------|------|-------|
| 列映射 | ✓ | △ |
| WIP限制 | ✓ | ✗ |
| 快速操作 | ✓ | ✗ |
| 泳道 | ✓ | ✗ |
| 快速筛选 | ✓ | ✗ |

### 8.2 设计 - Sprint支持

```sql
-- Sprint
CREATE TABLE sprint (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    sprint_name VARCHAR(200) NOT NULL,
    sprint_goal TEXT,
    start_date DATE,
    end_date DATE,
    goal TEXT,
    status VARCHAR(50) DEFAULT 'PLANNING');

-- Sprint Work Item
CREATE TABLE sprint_work_item (
    sprint_id BIGINT NOT NULL,
    work_item_id BIGINT NOT NULL,
    sprint_order INTEGER DEFAULT 0,
    PRIMARY KEY(sprint_id, work_item_id));
```

---

## 9. 通知系统

### 9.1 Jira 通知方案

Jira事件通知配置:
- Issue Created → Reporter, Assignee, Watchers
- Issue Assigned → Assignee
- Issue Resolved → Reporter, Assignee
- Comment Added → Watchers
- 更多...

**设计:**
```sql
-- 通知方案
CREATE TABLE notification_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    UNIQUE(tenant_id, scheme_key));

-- 通知事件配置
CREATE TABLE notification_event (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    recipients JSONB NOT NULL,
    template_key VARCHAR(100));
```

---

## 10. 优先实现功能清单

### Phase 1 - 核心功能 (MVP)
- [x] 工作项CRUD
- [x] 工作项类型
- [x] 自定义字段
- [x] 工作流
- [x] 项目管理
- [x] 基本视图

### Phase 2 - 扩展功能
- [ ] 字段上下文/配置方案
- [ ] 权限方案
- [ ] 工作流条件/验证器/后动作
- [ ] 自动化规则
- [ ] 过滤器/JQL
- [ ] Issue链接
- [ ] 时间跟踪增强

### Phase 3 - 企业功能
- [ ] 看板/WIP限制
- [ ] Sprint管理
- [ ] 通知系统
- [ ] 审计日志
- [ ] 附件存储
- [ ] 移动端支持

### Phase 4 - 高级功能
- [ ] AI智能建议
- [ ] Webhook集成
- [ ] 移动App
- [ ] 插件系统