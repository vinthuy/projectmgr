# Software Design Description - 工作项管理系统

## 1. 概述

### 1.1 目的
本文档描述工作项管理系统的软件架构、模块设计、接口设计和数据库设计，为开发团队提供详细的技术实现指导。

### 1.2 范围
实现一个支持动态自定义字段的工作项管理系统，包含：
- 工作项的CRUD操作
- 自定义字段的动态管理
- 动态列查询能力

## 2. 系统架构

### 2.1 技术栈
- 运行环境：JDK 17+
- 框架：Spring Boot 3.2
- ORM：MyBatis Plus 3.5
- 数据库：PostgreSQL
- 构建工具：Maven

### 2.2 架构模式
采用三层架构：
- Controller层：处理HTTP请求和响应
- Service层：处理业务逻辑
- Mapper层：处理数据持久化

## 3. 数据库设计

### 3.1 表结构

#### 3.1.1 工作项表 (work_item)
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

#### 3.1.2 字段定义表 (field_definition)
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

### 3.2 字段类型枚举
| 类型值 | 说明 |
|--------|------|
| TEXT | 文本输入 |
| NUMBER | 数字输入 |
| BOOLEAN | 布尔选择 |
| DATE | 日期选择 |
| SELECT | 单选下拉 |
| MULTI_SELECT | 多选 |

## 4. 接口设计

### 4.1 工作项接口

#### 4.1.1 创建工作项
```
POST /api/work-items
Request Body:
{
  "title": "string (required)",
  "description": "string",
  "status": "string",
  "priority": "string",
  "assignee": "string",
  "customFields": {}
}
Response: WorkItemResponse
```

#### 4.1.2 更新工作项
```
PUT /api/work-items/{id}
Request Body: WorkItemUpdateRequest
Response: WorkItemResponse
```

#### 4.1.3 删除工作项
```
DELETE /api/work-items/{id}
Response: void
```

#### 4.1.4 获取工作项
```
GET /api/work-items/{id}
Response: WorkItemResponse
```

#### 4.1.5 分页查询
```
GET /api/work-items?page=1&pageSize=20
Response: PageResponse<WorkItemResponse>
```

#### 4.1.6 动态列查询
```
POST /api/work-items/query
Request Body:
{
  "columns": ["title", "status", "customField1"],
  "condition": {"status": "OPEN"},
  "page": 1,
  "pageSize": 20
}
Response: PageResponse<Map>
```

#### 4.1.7 部分更新自定义字段
```
PATCH /api/work-items/{id}/custom-fields
Request Body: Map<String, Object>
Response: WorkItemResponse
```

### 4.2 字段定义接口

#### 4.2.1 创建字段定义
```
POST /api/field-definitions
Request Body: FieldDefinitionCreateRequest
Response: FieldDefinitionResponse
```

#### 4.2.2 更新字段定义
```
PUT /api/field-definitions/{id}
Request Body: FieldDefinitionUpdateRequest
Response: FieldDefinitionResponse
```

#### 4.2.3 删除字段定义
```
DELETE /api/field-definitions/{id}
Response: void
```

#### 4.2.4 获取字段定义
```
GET /api/field-definitions/{id}
Response: FieldDefinitionResponse
```

#### 4.2.5 字段定义列表
```
GET /api/field-definitions
Response: List<FieldDefinitionResponse>
```

## 5. 模块设计

### 5.1 Controller层
- `WorkItemController`: 工作项REST API
- `FieldDefinitionController`: 字段定义REST API
- `Result<T>`: 统一响应包装

### 5.2 Service层
- `WorkItemService`: 工作项业务逻辑
- `FieldDefinitionService`: 字段定义业务逻辑

### 5.3 Mapper层
- `WorkItemMapper`: 工作项数据访问
- `FieldDefinitionMapper`: 字段定义数据访问

### 5.4 实体类
- `WorkItem`: 工作项实体
- `FieldDefinition`: 字段定义实体
- `JacksonJsonTypeHandler`: JSONB类型处理器

### 5.5 DTO层
- 请求DTO：用于接收客户端请求
- 响应DTO：用于返回客户端数据
- 分页响应：统一的分页数据结构

## 6. 配置设计

### 6.1 application.yml
- 数据源配置
- MyBatis Plus配置
- 服务端口配置

## 7. 验收标准

### 7.1 功能验收
- [ ] 工作项支持创建、读取、更新、删除操作
- [ ] 自定义字段支持动态增加、删除、修改
- [ ] 支持动态列查询，可指定返回的字段
- [ ] 支持按自定义字段条件筛选
- [ ] 分页查询正常工作

### 7.2 技术验收
- [ ] 代码编译通过
- [ ] 单元测试覆盖核心业务逻辑
- [ ] 遵循RESTful API设计规范
- [ ] 统一的错误处理和响应格式

## 8. 目录结构

```
workitem-system/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/workitem/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   ├── service/
│   │   │   └── WorkItemApplication.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── mapper/
│   │       └── schema.sql
│   └── test/
└── docs/
    └── SDD.md
```
