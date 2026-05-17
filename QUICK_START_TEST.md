# 快速启动与测试指南

## 🚀 快速启动

### 方式一：使用启动脚本（推荐）

#### 1. 启动后端服务
```powershell
.\start-backend.ps1
```

#### 2. 启动前端服务（新终端窗口）
```powershell
.\start-frontend.ps1
```

### 方式二：手动启动

#### 1. 启动后端
```powershell
cd D:\code\java\meta
mvn spring-boot:run
```

#### 2. 启动前端（新终端）
```powershell
cd D:\code\java\meta\frontend
npm run dev
```

---

## 📋 启动前检查清单

### ✅ 环境要求
- [ ] JDK 21 已安装
- [ ] Maven 3.6+ 已安装
- [ ] Node.js 18+ 已安装
- [ ] PostgreSQL 17 已安装并运行
- [ ] 数据库 `workitem` 已创建

### ✅ 数据库准备
如果数据库未创建，执行：
```powershell
psql -U postgres -c "CREATE DATABASE workitem;"
```

数据库配置（application.yml）：
- 主机: localhost:5432
- 数据库: workitem
- 用户名: postgres
- 密码: 123

---

## 🧪 功能测试清单

### 1️⃣ 租户管理测试

**访问**: http://localhost:5173/config/tenants

| 测试项 | 操作 | 预期结果 | 状态 |
|--------|------|----------|------|
| 查看租户列表 | 打开页面 | 显示"默认租户" | ⬜ |
| 创建新租户 | 点击"新增租户"，填写信息 | 创建成功，列表刷新 | ⬜ |
| 编辑租户 | 点击"编辑"，修改信息 | 更新成功 | ⬜ |
| 删除租户 | 点击"删除"（非默认租户） | 删除成功 | ⬜ |
| 保护默认租户 | 尝试删除默认租户 | 提示不能删除 | ⬜ |

**测试数据示例**：
```
租户标识: test-company
租户名称: 测试公司
描述: 用于测试的租户
许可类型: FREE
最大用户数: 10
最大项目数: 5
```

---

### 2️⃣ 工作项类型配置测试

**访问**: http://localhost:5173/config/types

| 测试项 | 操作 | 预期结果 | 状态 |
|--------|------|----------|------|
| 切换租户 | 选择不同租户 | 显示对应租户的类型 | ⬜ |
| 查看默认类型 | 选择默认租户 | 显示EPIC, STORY, TASK等 | ⬜ |
| 数据隔离验证 | 切换租户后查看 | 不同类型互不影响 | ⬜ |

**API测试**：
```bash
# 获取默认租户的类型
curl http://localhost:8080/api/v1/work-item-types?tenantId=1

# 获取指定租户的类型
curl http://localhost:8080/api/v1/work-item-types?tenantId=2
```

---

### 3️⃣ 字段定义配置测试

**访问**: http://localhost:5173/config/fields

| 测试项 | 操作 | 预期结果 | 状态 |
|--------|------|----------|------|
| 切换租户 | 选择不同租户 | 显示对应租户的字段 | ⬜ |
| 创建字段 | 新增SELECT类型字段 | 创建成功 | ⬜ |
| 字段选项 | 输入多行选项 | 正确保存为数组 | ⬜ |
| 字段唯一性 | 同租户重复名称 | 提示已存在 | ⬜ |
| 跨租户同名 | 不同租户相同名称 | 允许创建 | ⬜ |

**测试数据示例**：
```
字段标识: priority_level
显示标签: 优先级
字段类型: SELECT
是否必填: 否
选项列表:
  LOW
  MEDIUM
  HIGH
  CRITICAL
```

---

### 4️⃣ 工作流状态配置测试

**访问**: http://localhost:5173/config/statuses

| 测试项 | 操作 | 预期结果 | 状态 |
|--------|------|----------|------|
| 切换租户 | 选择不同租户 | 显示对应租户的状态 | ⬜ |
| 查看默认状态 | 选择默认租户 | 显示OPEN, IN_PROGRESS等 | ⬜ |
| 状态分类 | 查看分类标签 | TO_DO, IN_PROGRESS, DONE | ⬜ |

**API测试**：
```bash
# 获取所有状态
curl http://localhost:8080/api/v1/workflow-statuses?tenantId=1

# 按分类获取
curl http://localhost:8080/api/v1/workflow-statuses/by-category/TO_DO?tenantId=1
```

---

## 🔍 API接口测试

### 租户管理API

```bash
# 1. 获取所有租户
GET http://localhost:8080/api/v1/tenants

# 2. 创建租户
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

# 3. 更新租户
PUT http://localhost:8080/api/v1/tenants/2
Content-Type: application/json

{
  "tenantName": "A公司（更新）",
  "maxUsers": 20
}

# 4. 删除租户
DELETE http://localhost:8080/api/v1/tenants/2
```

### 配置API（带租户参数）

```bash
# 工作项类型
GET http://localhost:8080/api/v1/work-item-types?tenantId=1

# 字段定义
GET http://localhost:8080/api/field-definitions?tenantId=1

# 工作流状态
GET http://localhost:8080/api/v1/workflow-statuses?tenantId=1
```

---

## 💾 数据库验证

### 检查租户数据
```sql
-- 查看所有租户
SELECT id, tenant_key, tenant_name, status, created_at 
FROM tenant 
WHERE deleted = false;

-- 查看各租户的配置数量
SELECT 
    t.tenant_name,
    COUNT(DISTINCT wit.id) as type_count,
    COUNT(DISTINCT fd.id) as field_count,
    COUNT(DISTINCT ws.id) as status_count
FROM tenant t
LEFT JOIN work_item_type wit ON t.id = wit.tenant_id AND wit.deleted = false
LEFT JOIN field_definition fd ON t.id = fd.tenant_id AND fd.deleted = false
LEFT JOIN workflow_status ws ON t.id = ws.tenant_id AND ws.deleted = false
GROUP BY t.id, t.tenant_name;
```

### 验证数据隔离
```sql
-- 租户1的类型
SELECT * FROM work_item_type WHERE tenant_id = 1 AND deleted = false;

-- 租户2的类型（如果创建了）
SELECT * FROM work_item_type WHERE tenant_id = 2 AND deleted = false;
```

---

## ⚠️ 常见问题

### Q1: 后端启动失败 - 数据库连接错误
**解决**: 
```powershell
# 检查PostgreSQL服务
Get-Service -Name postgresql*

# 启动服务
Start-Service postgresql-x64-17

# 创建数据库
psql -U postgres -c "CREATE DATABASE workitem;"
```

### Q2: 前端启动失败 - 依赖缺失
**解决**:
```powershell
cd frontend
npm install
```

### Q3: 页面空白或API请求失败
**检查**:
1. 后端是否正常启动（查看控制台日志）
2. 前端代理配置是否正确
3. 浏览器控制台是否有错误

### Q4: 租户选择器没有数据
**检查**:
1. 后端API是否正常返回租户列表
2. 浏览器Network面板查看请求
3. 数据库中是否有租户数据

---

## 📊 测试完成标准

- [ ] 能够创建至少2个租户
- [ ] 不同租户的配置完全隔离
- [ ] 切换租户时数据正确刷新
- [ ] 默认租户不能被删除
- [ ] 所有CRUD操作正常工作
- [ ] API响应格式正确

---

## 🎯 下一步

测试通过后，可以继续实现：
1. 项目级配置管理
2. 工作流引擎
3. 自动化规则
4. 权限控制系统

祝测试顺利！🎉
