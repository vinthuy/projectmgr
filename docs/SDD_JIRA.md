# SDD - 通用工作项管理系统 (Jira-Like)

## 1. 系统概述

### 1.1 目的
设计实现一个支持多租户、自定义工作项类型、自定义字段、工作流和自动化的企业级工作项管理平台。

### 1.2 核心功能范围
- 多租户支持
- 工作项类型自定义（Issue Type）
- 字段自定义（Custom Field）
- 工作流自定义（Workflow）
- 自动化规则（Automation）
- 类型模板（Type Scheme）
- 项目模板（Project Scheme）
- 通用工作项列表视图（Board/Filter）

---

## 2. 系统架构

### 2.1 技术栈
```
- JDK 21
- Spring Boot 3.3
- MyBatis-Flex 1.9
- PostgreSQL 17
- Redis (缓存/会话)
- MinIO (附件存储)
```

### 2.2 架构模式
```
┌─────────────────────────────────────────────────────┐
│                   Presentation Layer               │
│  (Controller / GraphQL / WebSocket)               │
├─────────────────────────────────────────────────────┤
│                   Application Layer               │
│  (Service / Event / Workflow Engine)              │
├─────────────────────────────────────────────────────┤
│                   Domain Layer                    │
│  (Entity / Aggregate / Value Object)              │
├─────────────────────────────────────────────────────┤
│                   Infrastructure Layer            │
│  (Mapper / Repository / External API)            │
└─────────────────────────────────────────────────────┘
```

### 2.3 模块划分

#### 核心模块
| 模块 | 说明 |
|-----|------|
| tenant-module | 多租户管理 |
| user-module | 用户/权限管理 |
| project-module | 项目/模板管理 |
| issuetype-module | 工作项类型管理 |
| field-module | 自定义字段管理 |
| workflow-module | 工作流引擎 |
| automation-module | 自动化规则引擎 |
| workitem-module | 核心工作项处理 |

---

## 3. 数据库设计

### 3.1 租户模块表

#### 3.1.1 租户表 (tenant)
```sql
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_key VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    license_type VARCHAR(50) DEFAULT 'FREE',
    max_users INTEGER DEFAULT 10,
    max_projects INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);
```

#### 3.1.2 用户表 (sys_user)
```sql
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL,
    display_name VARCHAR(200),
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);
```

#### 3.1.3 租户成员表 (tenant_member)
```sql
CREATE TABLE tenant_member (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(tenant_id, user_id)
);
```

### 3.2 项目模块表

#### 3.2.1 项目表 (project)
```sql
CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_key VARCHAR(50) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,
    lead_user_id BIGINT,
    type_scheme_id BIGINT,
    workflow_scheme_id BIGINT,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_key)
);
```

#### 3.2.2 项目成员表 (project_member)
```sql
CREATE TABLE project_member (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(project_id, user_id)
);
```

#### 3.2.3 项目模板表 (project_template)
```sql
CREATE TABLE project_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_key VARCHAR(50) NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    description TEXT,
    type_scheme_id BIGINT,
    workflow_scheme_id BIGINT,
    default_field_values JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, template_key)
);
```

### 3.3 工作项类型模块表

#### 3.3.1 工作项类型表 (issue_type)
```sql
CREATE TABLE issue_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    type_key VARCHAR(50) NOT NULL,
    type_name VARCHAR(200) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    type_category VARCHAR(50) DEFAULT 'STANDARD',
    hierarchy_level INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, type_key)
);
```

#### 3.3.2 类型方案表 (type_scheme)
```sql
CREATE TABLE type_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);
```

#### 3.3.3 类型方案关联表 (type_scheme_issue_type)
```sql
CREATE TABLE type_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    field_layout_id BIGINT,
    UNIQUE(scheme_id, issue_type_id)
);
```

### 3.4 字段模块表

#### 3.4.1 字段定义表 (field_definition)
```sql
CREATE TABLE field_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    required BOOLEAN DEFAULT FALSE,
    default_value JSONB,
    options JSONB,
    searcher_key VARCHAR(100),
    renderer_key VARCHAR(100),
    is_system BOOLEAN DEFAULT FALSE,
    is_global BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, field_key)
);
```

#### 3.4.2 字段布局表 (field_layout)
```sql
CREATE TABLE field_layout (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT,
    field_definition_id BIGINT NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    field_sequence INTEGER DEFAULT 0,
    is_visible BOOLEAN DEFAULT TRUE,
    is_editable BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    rendering_config JSONB,
    UNIQUE(scheme_id, field_definition_id, issue_type_id)
);
```

### 3.5 工作流模块表

#### 3.5.1 工作流定义表 (workflow_definition)
```sql
CREATE TABLE workflow_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workflow_key VARCHAR(50) NOT NULL,
    workflow_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, workflow_key, version)
);
```

#### 3.5.2 工作流步骤表 (workflow_step)
```sql
CREATE TABLE workflow_step (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    step_key VARCHAR(50) NOT NULL,
    step_name VARCHAR(200) NOT NULL,
    step_sequence INTEGER DEFAULT 0,
    status_category VARCHAR(50),
    UNIQUE(workflow_id, step_key)
);
```

#### 3.5.3 工作流转换表 (workflow_transition)
```sql
CREATE TABLE workflow_transition (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    transition_key VARCHAR(50) NOT NULL,
    transition_name VARCHAR(200) NOT NULL,
    source_step_id BIGINT NOT NULL,
    target_step_id BIGINT NOT NULL,
    transition_type VARCHAR(50) DEFAULT 'DIRECT',
    conditions JSONB,
    validators JSONB,
    post_functions JSONB,
    UNIQUE(workflow_id, transition_key)
);
```

#### 3.5.4 工作流方案表 (workflow_scheme)
```sql
CREATE TABLE workflow_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);
```

#### 3.5.5 工作流方案关联表 (workflow_scheme_issue_type)
```sql
CREATE TABLE workflow_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    UNIQUE(scheme_id, issue_type_id)
);
```

### 3.6 自动化模块表

#### 3.6.1 自动化规则表 (automation_rule)
```sql
CREATE TABLE automation_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(100) NOT NULL,
    project_id BIGINT,
    issue_type_id BIGINT,
    is_enabled BOOLEAN DEFAULT TRUE,
    execution_order INTEGER DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);
```

#### 3.6.2 自动化条件表 (automation_condition)
```sql
CREATE TABLE automation_condition (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    condition_config JSONB NOT NULL,
    logical_operator VARCHAR(10) DEFAULT 'AND',
    condition_sequence INTEGER DEFAULT 0
);
```

#### 3.6.3 自动化动作表 (automation_action)
```sql
CREATE TABLE automation_action (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    action_key VARCHAR(100) NOT NULL,
    action_config JSONB NOT NULL,
    action_sequence INTEGER DEFAULT 0
);
```

### 3.7 核心工作项表

#### 3.7.1 工作项表 (work_item)
```sql
CREATE TABLE work_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    workflow_step_id BIGINT,
    priority VARCHAR(50),
    resolution VARCHAR(50),
    assignee_user_id BIGINT,
    reporter_user_id BIGINT,
    parent_item_id BIGINT,
    custom_fields JSONB DEFAULT '{}',
    labels JSONB DEFAULT '[]',
    attachments JSONB DEFAULT '[]',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_id, item_key)
);

CREATE INDEX idx_work_item_project ON work_item(project_id);
CREATE INDEX idx_work_item_status ON work_item(status);
CREATE INDEX idx_work_item_assignee ON work_item(assignee_user_id);
CREATE INDEX idx_work_item_custom_fields ON work_item USING GIN(custom_fields);
```

#### 3.7.2 工作项历史表 (work_item_history)
```sql
CREATE TABLE work_item_history (
    id BIGSERIAL PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP DEFAULT NOW()
);
```

#### 3.7.3 工作项评论表 (work_item_comment)
```sql
CREATE TABLE work_item_comment (
    id BIGSERIAL PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    author_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);
```

### 3.8 视图/过滤器表

#### 3.8.1 过滤器表 (filter_view)
```sql
CREATE TABLE filter_view (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_user_id BIGINT NOT NULL,
    is_shared BOOLEAN DEFAULT FALSE,
    query_config JSONB NOT NULL,
    column_config JSONB,
    sort_config JSONB,
    group_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE
);
```

#### 3.8.2 面板配置表 (board_config)
```sql
CREATE TABLE board_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    board_name VARCHAR(200) NOT NULL,
    board_type VARCHAR(50) DEFAULT 'SPRINT',
    filter_view_id BIGINT,
    column_field_key VARCHAR(100),
    column_mapping JSONB,
    swimlane_config JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_id, board_name)
);
```

---

## 4. 核心接口设计

### 4.1 租户接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/tenants | 创建租户 |
| GET | /api/tenants/{id} | 获取租户 |
| PUT | /api/tenants/{id} | 更新租户 |
| GET | /api/tenants/{id}/users | 租户用户列表 |

### 4.2 项目接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/projects | 创建项目 |
| GET | /api/projects | 项目列表 |
| GET | /api/projects/{id} | 项目详情 |
| PUT | /api/projects/{id} | 更新项目 |
| DELETE | /api/projects/{id} | 删除项目 |
| POST | /api/projects/from-template | 从模板创建 |

### 4.3 工作项类型接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/issue-types | 创建类型 |
| GET | /api/issue-types | 类型列表 |
| GET | /api/issue-types/{id} | 类型详情 |
| PUT | /api/issue-types/{id} | 更新类型 |

### 4.4 字段定义接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/fields | 创建字段 |
| GET | /api/fields | 字段列表 |
| PUT | /api/fields/{id} | 更新字段 |
| DELETE | /api/fields/{id} | 删除字段 |
| GET | /api/fields/available | 可用字段列表 |

### 4.5 工作流接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/workflows | 创建工作流 |
| GET | /api/workflows | 工作流列表 |
| GET | /api/workflows/{id} | 工作流详情 |
| PUT | /api/workflows/{id} | 更新工作流 |
| POST | /api/workflows/{id}/transitions | 添加转换 |

### 4.6 自动化接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/automations | 创建规则 |
| GET | /api/automations | 规则列表 |
| PUT | /api/automations/{id} | 更新规则 |
| POST | /api/automations/{id}/enable | 启用规则 |
| POST | /api/automations/{id}/disable | 禁用规则 |

### 4.7 工作项接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/work-items | 创建工作项 |
| GET | /api/work-items | 工作项列表 |
| GET | /api/work-items/{id} | 工作项详情 |
| PUT | /api/work-items/{id} | 更新工作项 |
| DELETE | /api/work-items/{id} | 删除工作项 |
| POST | /api/work-items/{id}/transition | 状态转换 |
| POST | /api/work-items/{id}/comments | 添加评论 |

### 4.8 视图接口
| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/filters | 创建过滤器 |
| GET | /api/filters | 过滤器列表 |
| GET | /api/filters/{id} | 过滤器详情 |
| PUT | /api/filters/{id} | 更新过滤器 |
| GET | /api/boards | 面板列表 |
| POST | /api/boards | 创建面板 |

---

## 5. 领域模型设计

### 5.1 核心聚合根
```
Tenant (租户聚合根)
  └─ TenantMember

Project (项目聚合根)
  └─ ProjectMember

IssueType (工作项类型聚合根)
  └─ TypeScheme
       └─ TypeSchemeIssueType

WorkItem (工作项聚合根)
  ├─ WorkItemHistory
  └─ WorkItemComment

Workflow (工作流聚合根)
  ├─ WorkflowStep
  ├─ WorkflowTransition
  └─ WorkflowScheme

Automation (自动化聚合根)
  ├─ AutomationCondition
  └─ AutomationAction
```

### 5.2 值对象
```
FieldValue (字段值)
StatusCategory (状态分类)
Permission (权限)
WorkflowContext (工作流上下文)
```

---

## 6. 工作流引擎设计

### 6.1 状态机
```
工作流状态流转:
┌─────────┐     ┌──────────┐     ┌─────────┐
│  TODO   │────▶│ IN_PROGRESS │────▶│  DONE  │
└─────────┘     └──────────┘     └─────────┘
     │               │                 │
     └──────────────┴─────────────────┘
              (支持回退)
```

### 6.2 转换器类型
- DIRECT: 直接转换
- CONDITIONAL: 条件转换
- CIRCLE: 循环转换

---

## 7. 自动化引擎设计

### 7.1 支持的事件类型
```
- issue.created (工作项创建)
- issue.updated (工作项更新)
- issue.transitioned (状态转换)
- issue.commented (评论添加)
- field.changed (字段变更)
```

### 7.2 支持的动作
```
- transition (状态转换)
- assign (分配)
- notify (通知)
- set.field (设置字段值)
- create.issue (创建子工作项)
- webhook (调用外部 webhook)
```

---

## 8. 验收标准

### 8.1 多租户
- [ ] 支持租户隔离
- [ ] 支持租户配置管理

### 8.2 项目管理
- [ ] 支持项目创建/编辑/删除
- [ ] 支持从模板创建项目
- [ ] 支持项目成员管理

### 8.3 工作项类型
- [ ] 支持自定义类型
- [ ] 支持类型方案配置
- [ ] 支持类型图标配置

### 8.4 字段管理
- [ ] 支持自定义字段创建
- [ ] 支持字段布局配置
- [ ] 支持字段权限配置

### 8.5 工作流
- [ ] 支持可视化工作流设计
- [ ] 支持状态转换验证
- [ ] 支持转换后动作

### 8.6 自动化
- [ ] 支持触发规则配置
- [ ] 支持条件判断
- [ ] 支持动作执行

### 8.7 视图
- [ ] 支持过滤器/视图创建
- [ ] 支持列配置
- [ ] 支持看板视图