# SPEC.md - 软件规格说明书

## 1. 引言

### 1.1 目的
本文档定义工作项管理系统（Work Item Management System）的功能规格和验收标准，作为开发、测试和验收的依据。

### 1.2 范围
本系统是一个支持动态自定义字段的工作项管理平台，主要用于敏捷开发团队的任务跟踪和管理。

### 1.3 定义与缩写

| 术语 | 定义 |
|------|------|
| WI | Work Item，工作项 |
| FD | Field Definition，字段定义 |
| CRUD | Create/Read/Update/Delete，增删改查 |
| REST | Representational State Transfer，RESTful API风格 |
| JSONB | PostgreSQL JSON Binary，JSON二进制存储 |

## 2. 系统概述

### 2.1 项目背景
需要一个灵活的工作项管理系统，支持用户根据业务需求动态添加自定义字段，无需修改数据库结构。

### 2.2 系统目标
- 提供工作项的完整生命周期管理
- 支持动态自定义字段的灵活配置
- 提供动态列查询能力，满足多样化报表需求

### 2.3 用户角色
| 角色 | 权限 |
|------|------|
| 管理员 | 管理字段定义，管理所有工作项 |
| 普通用户 | 管理工作项，使用已定义的字段 |

## 3. 功能规格

### 3.1 工作项管理

#### 3.1.1 创建工作项
- **功能描述**: 创建新的工作项
- **输入字段**:
  - title (必填): 工作项标题，最大255字符
  - description (可选): 详细描述，文本类型
  - status (可选): 状态，默认"OPEN"
  - priority (可选): 优先级，默认"MEDIUM"
  - assignee (可选): 负责人
  - customFields (可选): 自定义字段，JSON对象格式
- **业务规则**:
  - title不能为空
  - 自定义字段的值必须匹配已定义的字段类型
- **输出**: 创建成功的工作项信息

#### 3.1.2 查询工作项
- **功能描述**: 获取单个工作项详情
- **输入**: 工作项ID
- **输出**: 包含所有字段和可用字段列表的工作项详情

#### 3.1.3 更新工作项
- **功能描述**: 修改工作项信息
- **输入**: 工作项ID + 更新字段
- **业务规则**:
  - 支持部分更新，只修改传入的字段
  - 自定义字段支持增量更新（合并而非覆盖）
- **输出**: 更新后的工作项信息

#### 3.1.4 删除工作项
- **功能描述**: 软删除工作项
- **输入**: 工作项ID
- **输出**: 操作成功标识

#### 3.1.5 分页查询
- **功能描述**: 分页获取工作项列表
- **输入参数**:
  - page: 页码，默认1
  - pageSize: 每页数量，默认20
- **输出**: 分页结果，包含记录列表和分页信息

#### 3.1.6 动态列查询
- **功能描述**: 按指定列查询工作项
- **输入参数**:
  - columns: 要返回的字段列表
  - condition: 筛选条件，键值对格式
  - page: 页码（可选）
  - pageSize: 每页数量（可选）
- **业务规则**:
  - columns支持基础字段和自定义字段
  - condition中自定义字段通过key直接筛选
- **输出**: 符合条件的工作项指定列数据

#### 3.1.7 部分更新自定义字段
- **功能描述**: 单独更新工作项的自定义字段
- **输入**: 工作项ID + 自定义字段映射
- **业务规则**: 增量更新，不影响其他自定义字段

### 3.2 字段定义管理

#### 3.2.1 创建字段定义
- **功能描述**: 定义新的自定义字段
- **输入字段**:
  - fieldName (必填): 字段标识，字母数字下划线，唯一
  - fieldType (必填): 字段类型
  - fieldLabel (必填): 显示标签
  - required (可选): 是否必填，默认false
  - defaultValue (可选): 默认值
  - options (可选): 下拉选项，用于SELECT/MULTI_SELECT类型
- **字段类型枚举**:
  | 类型 | 说明 | 选项支持 |
  |------|------|----------|
  | TEXT | 单行文本 | 否 |
  | NUMBER | 数字 | 否 |
  | BOOLEAN | 布尔值 | 否 |
  | DATE | 日期 | 否 |
  | SELECT | 单选下拉 | 是 |
  | MULTI_SELECT | 多选下拉 | 是 |
- **输出**: 创建的字段定义

#### 3.2.2 更新字段定义
- **功能描述**: 修改字段定义属性
- **可更新字段**: fieldLabel, required, defaultValue, options
- **业务规则**:
  - 不允许修改fieldName和fieldType
  - 删除字段定义不影响已有工作项的数据

#### 3.2.3 删除字段定义
- **功能描述**: 删除字段定义
- **输入**: 字段定义ID
- **业务规则**: 软删除，已创建的工作项保留该字段数据

#### 3.2.4 查询字段定义
- **功能描述**: 获取单个字段定义
- **输入**: 字段定义ID
- **输出**: 字段定义详情

#### 3.2.5 列表字段定义
- **功能描述**: 获取所有字段定义列表
- **输出**: 字段定义列表

### 3.3 系统状态值

#### 3.3.1 工作项状态
| 状态值 | 说明 |
|--------|------|
| OPEN | 待处理 |
| IN_PROGRESS | 进行中 |
| RESOLVED | 已解决 |
| CLOSED | 已关闭 |

#### 3.3.2 优先级
| 优先级值 | 说明 |
|----------|------|
| LOW | 低 |
| MEDIUM | 中 |
| HIGH | 高 |
| CRITICAL | 紧急 |

## 4. 接口规格

### 4.1 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 状态码，200成功，4xx客户端错误，5xx服务端错误 |
| message | string | 状态消息 |
| data | object | 响应数据 |

### 4.2 API端点

#### 工作项接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/work-items | 创建工作项 |
| GET | /api/work-items/{id} | 获取工作项详情 |
| PUT | /api/work-items/{id} | 更新工作项 |
| DELETE | /api/work-items/{id} | 删除工作项 |
| GET | /api/work-items | 分页查询工作项 |
| POST | /api/work-items/query | 动态列查询 |
| PATCH | /api/work-items/{id}/custom-fields | 部分更新自定义字段 |

#### 字段定义接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/field-definitions | 创建字段定义 |
| GET | /api/field-definitions/{id} | 获取字段定义 |
| PUT | /api/field-definitions/{id} | 更新字段定义 |
| DELETE | /api/field-definitions/{id} | 删除字段定义 |
| GET | /api/field-definitions | 列表字段定义 |

## 5. 数据规格

### 5.1 数据库表

#### work_item 表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| title | VARCHAR(255) | NOT NULL | 标题 |
| description | TEXT | | 描述 |
| status | VARCHAR(50) | DEFAULT 'OPEN' | 状态 |
| priority | VARCHAR(50) | DEFAULT 'MEDIUM' | 优先级 |
| assignee | VARCHAR(100) | | 负责人 |
| custom_fields | JSONB | DEFAULT '{}' | 自定义字段 |
| created_at | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| deleted | BOOLEAN | DEFAULT FALSE | 逻辑删除 |

#### field_definition 表
| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 主键 |
| field_name | VARCHAR(100) | NOT NULL, UNIQUE | 字段标识 |
| field_type | VARCHAR(50) | NOT NULL | 字段类型 |
| field_label | VARCHAR(100) | NOT NULL | 显示标签 |
| required | BOOLEAN | DEFAULT FALSE | 是否必填 |
| default_value | TEXT | | 默认值 |
| options | JSONB | | 下拉选项 |
| created_at | TIMESTAMP | DEFAULT NOW() | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT NOW() | 更新时间 |
| deleted | BOOLEAN | DEFAULT FALSE | 逻辑删除 |

### 5.2 索引

| 表 | 索引名 | 字段 | 说明 |
|----|--------|------|------|
| work_item | idx_work_item_status | status | 状态查询优化 |
| work_item | idx_work_item_priority | priority | 优先级查询优化 |
| work_item | idx_work_item_assignee | assignee | 负责人查询优化 |
| work_item | idx_work_item_created_at | created_at | 时间排序优化 |
| work_item | idx_work_item_custom_fields | custom_fields | GIN索引，支持JSON查询 |
| field_definition | idx_field_definition_name | field_name | 字段名唯一性 |

## 6. 非功能规格

### 6.1 性能要求
- 单条工作项查询响应时间 < 100ms
- 分页查询（20条）响应时间 < 200ms
- 动态列查询响应时间 < 500ms

### 6.2 可用性要求
- 系统可用性 >= 99.5%
- 支持JSON格式的RESTful API

### 6.3 安全要求
- 输入参数进行有效性校验
- SQL注入防护（使用参数化查询）

## 7. 验收标准

### 7.1 功能验收清单

- [ ] 工作项创建：可创建包含基础字段和自定义字段的工作项
- [ ] 工作项查询：可根据ID获取完整工作项信息
- [ ] 工作项更新：支持部分更新，自定义字段增量更新
- [ ] 工作项删除：软删除，不物理删除数据
- [ ] 工作项列表：分页展示，支持排序
- [ ] 动态列查询：可指定返回字段和筛选条件
- [ ] 字段定义创建：可创建各种类型的自定义字段
- [ ] 字段定义更新：可更新字段的元数据
- [ ] 字段定义删除：软删除，已有数据不受影响
- [ ] 字段定义列表：展示所有可用字段

### 7.2 接口验收清单

- [ ] 所有接口返回统一格式的JSON响应
- [ ] 错误情况返回合适的HTTP状态码
- [ ] 输入参数校验返回明确的错误信息

### 7.3 数据验收清单

- [ ] 自定义字段数据以JSONB格式正确存储
- [ ] GIN索引正确创建，支持JSON查询
- [ ] 逻辑删除正常工作

## 8. 附录

### 8.1 默认字段定义

系统初始化时创建的默认字段：

| fieldName | fieldType | fieldLabel | options |
|-----------|-----------|------------|---------|
| severity | SELECT | Severity | [LOW, MEDIUM, HIGH, CRITICAL] |
| component | TEXT | Component | - |
| version | TEXT | Version | - |
| tags | MULTI_SELECT | Tags | [bug, feature, enhancement, documentation] |

### 8.2 API使用示例

#### 创建工作项
```bash
POST /api/work-items
Content-Type: application/json

{
  "title": "Implement login feature",
  "description": "Add user authentication",
  "status": "OPEN",
  "priority": "HIGH",
  "assignee": "john.doe",
  "customFields": {
    "severity": "HIGH",
    "component": "security",
    "tags": ["feature"]
  }
}
```

#### 动态列查询
```bash
POST /api/work-items/query
Content-Type: application/json

{
  "columns": ["title", "status", "severity", "component"],
  "condition": {
    "status": "OPEN",
    "severity": "HIGH"
  }
}
```
