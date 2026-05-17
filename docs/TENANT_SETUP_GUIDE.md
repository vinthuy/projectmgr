# 租户级配置管理功能 - 启动指南

## 功能概述

本次实现完成了基于SDD_JIRA.md设计的**租户级配置管理系统**，包括：

### 1. 后端实现
- ✅ 租户表（tenant）及CRUD接口
- ✅ 工作项类型（work_item_type）支持租户隔离
- ✅ 字段定义（field_definition）支持租户隔离  
- ✅ 工作流状态（workflow_status）支持租户隔离
- ✅ 所有配置API支持tenantId参数

### 2. 前端实现
- ✅ 租户管理页面（/config/tenants）
- ✅ 工作项类型配置页添加租户选择器
- ✅ 字段定义配置页添加租户选择器
- ✅ 工作流状态配置页添加租户选择器
- ✅ 侧边栏菜单集成租户管理入口

## 启动步骤

### 第一步：初始化数据库

```powershell
# 在PostgreSQL中执行schema.sql
psql -U postgres -d workitem_db -f src/main/resources/schema.sql
```

或者使用项目提供的脚本：
```powershell
.\setup-database.ps1
```

### 第二步：启动后端服务

```bash
cd D:\code\java\meta
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

### 第三步：启动前端服务

```bash
cd D:\code\java\meta\frontend
npm install  # 首次运行需要
npm run dev
```

前端将在 http://localhost:5173 启动

## 功能测试

### 1. 租户管理测试

访问：http://localhost:5173/config/tenants

**测试场景：**
- ✅ 查看默认租户列表（应显示"默认租户"）
- ✅ 创建新租户（例如：tenant-key = "test-company", tenant-name = "测试公司"）
- ✅ 编辑租户信息
- ✅ 删除租户（默认租户不能删除）

### 2. 工作项类型配置测试

访问：http://localhost:5173/config/types

**测试场景：**
- ✅ 切换不同租户，查看各自的工作项类型
- ✅ 为不同租户创建不同类型
- ✅ 验证数据隔离（租户A的类型不会出现在租户B）

### 3. 字段定义配置测试

访问：http://localhost:5173/config/fields

**测试场景：**
- ✅ 切换租户，查看各自的字段定义
- ✅ 为不同租户创建自定义字段
- ✅ 验证字段名称在同一租户内唯一

### 4. 工作流状态配置测试

访问：http://localhost:5173/config/statuses

**测试场景：**
- ✅ 切换租户，查看各自的工作流状态
- ✅ 为不同租户配置不同的状态流程

## API测试

### 租户管理API

```bash
# 获取所有租户
GET http://localhost:8080/api/v1/tenants

# 创建租户
POST http://localhost:8080/api/v1/tenants
Content-Type: application/json
{
  "tenantKey": "company-a",
  "tenantName": "A公司",
  "description": "A公司租户",
  "licenseType": "FREE",
  "maxUsers": 10,
  "maxProjects": 5
}

# 更新租户
PUT http://localhost:8080/api/v1/tenants/2
Content-Type: application/json
{
  "tenantName": "A公司（已更新）",
  "maxUsers": 20
}

# 删除租户
DELETE http://localhost:8080/api/v1/tenants/2
```

### 工作项类型API（带租户参数）

```bash
# 获取指定租户的工作项类型
GET http://localhost:8080/api/v1/work-item-types?tenantId=1

# 获取指定租户的特定类型
GET http://localhost:8080/api/v1/work-item-types/TASK?tenantId=1
```

### 字段定义API（带租户参数）

```bash
# 获取指定租户的字段定义
GET http://localhost:8080/api/field-definitions?tenantId=1

# 创建字段（指定租户）
POST http://localhost:8080/api/field-definitions?tenantId=1
Content-Type: application/json
{
  "fieldName": "priority_level",
  "fieldLabel": "优先级",
  "fieldType": "SELECT",
  "required": false,
  "options": ["LOW", "MEDIUM", "HIGH"]
}
```

### 工作流状态API（带租户参数）

```bash
# 获取指定租户的工作流状态
GET http://localhost:8080/api/v1/workflow-statuses?tenantId=1

# 按分类获取状态
GET http://localhost:8080/api/v1/workflow-statuses/by-category/TO_DO?tenantId=1
```

## 数据库验证

### 检查租户数据

```sql
-- 查看所有租户
SELECT * FROM tenant WHERE deleted = false;

-- 查看各租户的工作项类型数量
SELECT t.tenant_name, COUNT(wit.id) as type_count
FROM tenant t
LEFT JOIN work_item_type wit ON t.id = wit.tenant_id AND wit.deleted = false
GROUP BY t.id, t.tenant_name;

-- 查看各租户的字段定义数量
SELECT t.tenant_name, COUNT(fd.id) as field_count
FROM tenant t
LEFT JOIN field_definition fd ON t.id = fd.tenant_id AND fd.deleted = false
GROUP BY t.id, t.tenant_name;

-- 查看各租户的工作流状态数量
SELECT t.tenant_name, COUNT(ws.id) as status_count
FROM tenant t
LEFT JOIN workflow_status ws ON t.id = ws.tenant_id AND ws.deleted = false
GROUP BY t.id, t.tenant_name;
```

## 关键特性

### 1. 数据隔离
- 每个租户的配置数据完全隔离
- 通过tenant_id字段实现多租户
- 唯一约束改为(tenant_id, code)组合

### 2. 默认租户
- 系统自动创建"默认租户"（id=1, tenant_key="default"）
- 所有API默认使用tenantId=1
- 默认租户不能被删除

### 3. 向后兼容
- 所有API保持向后兼容
- 不传tenantId时默认使用租户1
- 现有代码无需大幅修改

### 4. 前端体验
- 统一的租户选择器UI
- 实时切换租户查看配置
- 清晰的租户标识和名称显示

## 常见问题

### Q1: 为什么我的配置没有显示？
A: 检查是否选择了正确的租户。不同租户的配置是隔离的。

### Q2: 如何为新租户初始化默认配置？
A: 可以在TenantService.create()方法中添加逻辑，自动为新租户复制默认配置。

### Q3: 能否跨租户共享配置？
A: 当前设计不支持。如需共享，可以添加"is_global"字段或配置模板功能。

### Q4: 默认租户能删除吗？
A: 不能。前端和后端都做了保护，防止删除默认租户。

## 下一步扩展

根据SDD_JIRA.md，后续可以实现：

1. **项目级配置** - 在项目级别覆盖租户配置
2. **配置模板** - 支持从模板快速初始化租户配置
3. **配置导入导出** - 支持配置的备份和迁移
4. **权限控制** - 基于租户的细粒度权限管理
5. **审计日志** - 记录租户配置的变更历史

## 技术栈

- **后端**: Spring Boot 3.3 + MyBatis-Flex 1.9 + PostgreSQL 17
- **前端**: Vue 3 + Element Plus + Vite
- **架构**: 多租户SaaS架构，数据隔离模式

---

**开发完成时间**: 2026-04-12
**符合规范**: SDD_JIRA.md 第3章数据库设计、第4章接口设计
