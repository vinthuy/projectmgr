# SPEC - 通用工作项管理系统规格说明书

## 1. 引言

### 1.1 目的
定义Jira类通用工作项管理系统的功能规格，非功能规格和验收标准。

### 1.2 范围
企业级多租户工作项管理平台，支持工作项类型自定义、字段自定义、工作流自定义、自动化规则和项目模板。

### 1.3 定义与缩写

| 术语 | 定义 |
|-----|------|
| Tenant | 租户 |
| Issue/WorkItem | 工作项 |
| Issue Type | 工作项类型 |
| Workflow | 工作流 |
| Automation | 自动化规则 |
| Board | 看板视图 |

---

## 2. 功能规格

### 2.1 多租户管理

#### 2.1.1 租户创建
- 输入：租户名称、租户Key、管理员用户
- 业务规则：
  - 租户Key全局唯一
  - 创建时自动初始化默认配置
- 输出：租户信息

#### 2.1.2 租户配置
- 支持配置租户属性
- 支持设置租户 license 类型
- 支持配置用户配额

### 2.2 用户与权限

#### 2.2.1 用户管理
- **功能描述**：管理租户内的用户
- **用户角色**：
  | 角色 | 权限 |
  |-----|------|
  | 管理员 | 租户管理、系统配置 |
  | 项目管理员 | 项目管理 |
  | 普通用户 | 创建/编辑工作项 |

#### 2.2.2 权限模型
```
权限层级:
tenant_global > project > issue
```

### 2.3 项目管理

#### 2.3.1 项目创建
- **输入字段**：
  - projectKey (必填): 项目键，最大10字符
  - projectName (必填): 项目名称
  - leadUserId (可选): 项目负责人
  - typeSchemeId (可选): 类型方案
  - workflowSchemeId (可选): 工作流方案
- **业务规则**：
  - projectKey 租户内唯一
  - 自动关联默认模板

#### 2.3.2 项目模板
- **预置模板**：
  | 模板Key | 说明 |
  |--------|------|
  | scrum | Scrum敏捷开发 |
  | kanban | 看板管理 |
  | simple | 简单任务管理 |
  | bug-tracking | Bug追踪 |

### 2.4 工作项类型管理

#### 2.4.1 内置类型
| 类型Key | 类型名称 | 图标 | 说明 |
|--------|--------|-----|------|
| task | 任务 | task | 标准任务 |
| bug | 问题 | bug | 缺陷报告 |
| story | 用户故事 | bookmark | 敏捷Story |
| epic | 史诗 | star | 大型功能 |
| sub-task | 子任务 | sub-task | 子任务 |

#### 2.4.2 自定义类型
- 支持自定义类型名称
- 支持选择类型图标
- 支持设置类型分类
- 支持设置层级

#### 2.4.3 类型方案
- 将多个类型组合为方案
- 分配给项目使用

### 2.5 字段管理

#### 2.5.1 内置字段
| fieldKey | 字段名称 | 类型 |
|----------|---------|------|
| summary | 摘要 | TEXT |
| description | 描述 | RICHTEXT |
| status | 状态 | SELECT |
| priority | 优先级 | SELECT |
| assignee | 负责人 | USER |
| reporter | 报告人 | USER |
| labels | 标签 | MULTI_SELECT |
| components | 组件 | MULTI_SELECT |
| fixVersions | 修复版本 | MULTI_SELECT |
| dueDate | 截止日期 | DATE |
| timeEstimate | 预估时间 | NUMBER |
| timeSpent | 花费时间 | NUMBER |

#### 2.5.2 可用字段类型
| 类型 | dataType | 说明 |
|------|---------|------|
| TEXT | text | 单行文本 |
| RICHTEXT | html | 富文本 |
| NUMBER | number | 数字 |
| SELECT | text | 单选 |
| MULTI_SELECT | array | 多选 |
| USER | user | 用户选择 |
| DATE | datetime | 日期时间 |
| URL | url | 链接 |
| LABELS | array | 标签 |

#### 2.5.3 字段布局
- 配置字段在详情页的显示顺序
- 配置字段是否可见/可编辑
- 支持按问题类型差异化配置

### 2.6 工作流管理

#### 2.6.1 内置工作流
| 工作流Key | 工作流名称 | ��明 |
|-----------|-----------|------|
| default | 默认工作流 | 标准的TODO>IN_PROGRESS>DONE流程 |
| bug | Bug工作流 | 包含reopened状态 |

#### 2.6.2 默认状态
| 状态Key | 状态名称 | 状态分类 |
|--------|---------|----------|
| TODO | 待处理 | OPEN |
| IN_PROGRESS | 进行中 | OPEN |
| IN_REVIEW | 审核中 | OPEN |
| DONE | 已完成 | DONE |
| CLOSED | 已关闭 | CLOSED |
| REOPENED | 已重开 | OPEN |

#### 2.6.3 转换动作
- 支持配置转换条件
- 支持设置转换验证器
- 支持执行转换后动作

### 2.7 自动化规则

#### 2.7.1 支持的事件
| 事件Key | 说明 |
|--------|------|
| issue.created | 创建时触发 |
| issue.updated | 更新时触发 |
| issue.transitioned | 状态转换时触发 |
| field.changed | 字段变更时触发 |

#### 2.7.2 支持的条件
- 字段值等于/不等于
- 字段值包含/不包含
- 是/不是指定用户
- 在特定项目中
- 是特定问题类型

#### 2.7.3 支持的动作
| 动作Key | 说明 |
|--------|------|
| transition | 状态转换 |
| assign | 分配给用户 |
| set_field | 设置字段值 |
| notify | 发送通知 |
| create_subtask | 创建子任务 |
| webhook | 调用外部API |

### 2.8 工作项管理

#### 2.8.1 创建工作项
- 自动生成ItemKey（项目Key-序号）
- 支持批量创建
- 支持从模板创建

#### 2.8.2 工作项生命周期
```
创建 -> TODO -> IN_PROGRESS -> IN_REVIEW -> DONE -> CLOSED
                              ↘ REOPENED ↗
```

#### 2.8.3 字段权限
- 内置字段按角色控制
- 自定义字段按字段布局控制

### 2.9 视图管理

#### 2.9.1 过滤器
- 支持JQL风格查询
- 支持保存为个人/共享视图
- 支持列配置
- 支持排序配置

#### 2.9.2 看板
- 支持按状态列分组
- 支持按泳道分组
- 支持WIP限制
- 支持快速操作

---

## 3. 接口规格

### 3.1 统一响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 3.2 错误响应格式
```json
{
  "code": 400,
  "message": "错误描述",
  "error": "ERROR_CODE"
}
```

### 3.3 分页响应格式
```json
{
  "code": 200,
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

---

## 4. 数据规格

### 4.1 字段权限配置
```json
{
  "fieldKey": "custom_field",
  "roles": ["admin", "user"],
  "permissions": {
    "create": true,
    "edit": true,
    "view": true
  }
}
```

### 4.2 工作流转换配置
```json
{
  "transitionKey": "start_progress",
  "fromStep": "TODO",
  "toStep": "IN_PROGRESS",
  "conditions": [],
  "validators": ["assignee_required"],
  "postFunctions": ["clear_resolution"]
}
```

### 4.3 自动化规则配置
```json
{
  "name": "自动分配Bug",
  "event": "issue.created",
  "conditions": [
    {"field": "issueType", "operator": "equals", "value": "bug"}
  ],
  "actions": [
    {"action": "assign", "value": "{{currentUser}}"}
  ]
}
```

---

## 5. 非功能规格

### 5.1 性能要求
- 单条工作项查询: < 50ms
- 工作项列表查询(100条): < 200ms
- 状态转换: < 100ms
- 页面加载: < 1s

### 5.2 可用性要求
- 系统可用性: >= 99.9%
- 支持RESTful API
- 支持GraphQL API(可选)

### 5.3 安全要求
- 密码加密存储
- API Token认证
- 租户数据隔离
- SQL注入防护
- XSS防护

---

## 6. 验收标准

### 6.1 租户管理验收
- [ ] 能够创建租户
- [ ] 能够管理租户配置
- [ ] 能够分配用户到租户

### 6.2 项目管理验收
- [ ] 能够创建项目
- [ ] 能够从模板创建项目
- [ ] 能够管理项目成员
- [ ] 能够配置项目方案

### 6.3 工作项类型验收
- [ ] 能够使用内置类型
- [ ] 能够创建自定义类型
- [ ] 能够配置类型方案

### 6.4 字段管理验收
- [ ] 能够使用内置字段
- [ ] 能够创建自定义字段
- [ ] 能够配置字段布局

### 6.5 工作流验收
- [ ] 能够使用内置工作流
- [ ] 能够创建自定义工作流
- [ ] 能够配置状态转换

### 6.6 自动化验收
- [ ] 能够创建自动化规则
- [ ] 能够配置触发条件
- [ ] 能够配置执行动作

### 6.7 工作项验收
- [ ] 能够创建工作项
- [ ] 能够编辑工作项
- [ ] 能够进行状态转换
- [ ] 能够添加评论
- [ ] 能够查看历史

### 6.8 视图验收
- [ ] 能够创建过滤器
- [ ] 能够保存查询
- [ ] 能够配置列
- [ ] 能够创建看板

---

## 7. 附录

### 7.1 内置问题类型配置
```json
{
  "issueTypes": [
    {"typeKey": "task", "name": "任务", "icon": "task"},
    {"typeKey": "bug", "name": "问题", "icon": "bug"},
    {"typeKey": "story", "name": "用户故事", "icon": "bookmark"},
    {"typeKey": "epic", "name": "史诗", "icon": "star"}
  ]
}
```

### 7.2 内置工作流配置
```json
{
  "workflows": [
    {
      "workflowKey": "default",
      "steps": [
        {"key": "TODO", "name": "待处理", "category": "OPEN"},
        {"key": "IN_PROGRESS", "name": "进行中", "category": "OPEN"},
        {"key": "DONE", "name": "已完成", "category": "DONE"}
      ],
      "transitions": [
        {"key": "start", "from": "TODO", "to": "IN_PROGRESS"},
        {"key": "complete", "from": "IN_PROGRESS", "to": "DONE"},
        {"key": "reopen", "from": "DONE", "to": "TODO"}
      ]
    }
  ]
}
```