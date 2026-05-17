# 字段上下文设计文档

## 1. 概述

### 1.1 目的
支持字段在不同项目/类型中有不同的配置，包括：
- 默认值差异
- 可选值差异
- 是否必填差异
- 显示名称差异

### 1.2 设计目标
```
Jira模式:
┌──────────────────────────────────────────────────────────┐
│                   Field Definition (全局)                   │
│  field_key: priority                                    │
│  field_name: 优先级                                   │
│  field_type: SELECT                                   │
└──────────────────────────────────────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│  Context: Dev   │ │ Context: QA     │ │ Context: Ops   │
│  - options:    │ │  - options:   │ │  - options:  │
│    [P1,P2,P3] │ │  [Blocker,    │ │  [Sev1,Sev2] │
│  - default:   │ │   Critical,   │ │  - default: │
│    P3         │ │   Major,Min] │ │    Sev2     │
│               │ │  - default:  │ │             │
│               │ │    Critical  │ │             │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## 2. 数据模型

### 2.1 核心表

```sql
-- =============================================
-- Field Context Tables
-- =============================================

-- 字段上下文
DROP TABLE IF EXISTS field_context CASCADE;
CREATE TABLE field_context (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    context_name VARCHAR(200) NOT NULL,
    context_type VARCHAR(50) DEFAULT 'PROJECT',
    context_ref_id BIGINT,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, field_definition_id, context_type, context_ref_id)
);

COMMENT ON TABLE field_context IS '字段上下文 - 支持按项目/类型配置不同选项';

-- 字段上下文配置
DROP TABLE IF EXISTS field_context_config CASCADE;
CREATE TABLE field_context_config (
    id BIGSERIAL PRIMARY KEY,
    context_id BIGINT NOT NULL REFERENCES field_context(id),
    default_value JSONB,
    placeholder VARCHAR(500),
    min_value NUMERIC,
    max_value NUMERIC,
    allowed_extensions VARCHAR(500),
    max_file_size BIGINT,
    rendering_config JSONB
);

-- 字段上下文选项（用于SELECT/MULTI_SELECT）
DROP TABLE IF EXISTS field_context_option CASCADE;
CREATE TABLE field_context_option (
    id BIGSERIAL PRIMARY KEY,
    context_id BIGINT NOT NULL REFERENCES field_context(id),
    option_key VARCHAR(100) NOT NULL,
    option_value VARCHAR(500) NOT NULL,
    option_order INTEGER DEFAULT 0,
    is_enabled BOOLEAN DEFAULT TRUE,
    UNIQUE(context_id, option_key)
);

-- 字段屏幕配置（控制字段在screen中的显示）
DROP TABLE IF EXISTS field_screen_config CASCADE;
CREATE TABLE field_screen_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    screen_id BIGINT NOT NULL,
    field_definition_id BIGINT NOT NULL,
    field_position INTEGER DEFAULT 0,
    is_visible BOOLEAN DEFAULT TRUE,
    is_editable BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    is_searchable BOOLEAN DEFAULT TRUE,
    column_name VARCHAR(50) DEFAULT 'PRIMARY',
    UNIQUE(screen_id, field_definition_id)
);
```

### 2.2 屏幕方案

```sql
-- 屏幕
DROP TABLE IF EXISTS screen CASCADE;
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    screen_key VARCHAR(50) NOT NULL,
    screen_name VARCHAR(200) NOT NULL,
    screen_type VARCHAR(50) DEFAULT 'EDIT',
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, screen_key)
);

-- 屏幕类型枚举
COMMENT ON COLUMN screen.screen_type IS 'EDIT|CREATE|VIEW';

-- 屏幕方案
DROP TABLE IF EXISTS screen_scheme CASCADE;
CREATE TABLE screen_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, scheme_key)
);

-- 屏幕方案关联（issue type -> screen）
DROP TABLE IF EXISTS screen_scheme_issue_type CASCADE;
CREATE TABLE screen_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    create_screen_id BIGINT,
    edit_screen_id BIGINT,
    view_screen_id BIGINT,
    UNIQUE(scheme_id, issue_type_id)
);
```

---

## 3. API设计

### 3.1 字段上下文API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/fields/{id}/contexts | 创建上下文 |
| GET | /api/fields/{id}/contexts | 获取上下文列表 |
| PUT | /api/fields/{id}/contexts/{contextId} | 更新上下文 |
| DELETE | /api/fields/{id}/contexts/{contextId} | 删除上下文 |
| POST | /api/fields/{id}/contexts/{contextId}/options | 添加选项 |
| DELETE | /api/fields/{id}/contexts/{contextId}/options/{optionKey} | 删除选项 |

### 3.2 屏幕API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/screens | 创建屏幕 |
| GET | /api/screens | 屏幕列表 |
| GET | /api/screens/{id} | 屏幕详情 |
| PUT | /api/screens/{id} | 更新屏幕 |
| POST | /api/screens/{id}/fields | 添加字段到屏幕 |
| DELETE | /api/screens/{id}/fields/{fieldId} | 从屏幕移除字段 |
| POST | /api/screen-schemes | 创建屏幕方案 |
| GET | /api/screen-schemes | 屏幕方案列表 |

### 3.3 请求/响应示例

**创建字段上下文:**
```json
POST /api/fields/priority/contexts
Request:
{
  "contextName": "开发项目",
  "contextType": "PROJECT",
  "contextRefId": 1,
  "defaultValue": "P3",
  "options": [
    {"optionKey": "P1", "optionValue": "P1 - 紧急", "optionOrder": 1},
    {"optionKey": "P2", "optionValue": "P2 - 高", "optionOrder": 2},
    {"optionKey": "P3", "optionValue": "P3 - 中", "optionOrder": 3},
    {"optionKey": "P4", "optionValue": "P4 - 低", "optionOrder": 4}
  ]
}

Response:
{
  "code": 200,
  "data": {
    "id": 100,
    "contextName": "开发项目",
    "contextType": "PROJECT",
    "contextRefId": 1,
    "options": [...]
  }
}
```

**创建屏幕:**
```json
POST /api/screens
Request:
{
  "screenKey": "bug-edit",
  "screenName": "Bug编辑屏幕",
  "screenType": "EDIT",
  "fields": [
    {"fieldKey": "summary", "fieldPosition": 1, "isRequired": true},
    {"fieldKey": "description", "fieldPosition": 2, "isRequired": false},
    {"fieldKey": "priority", "fieldPosition": 3, "isRequired": false},
    {"fieldKey": "assignee", "fieldPosition": 4, "isRequired": false}
  ]
}
```

---

## 4. 字段获取逻辑

### 4.1 核心服务

```java
public class FieldContextService {
    
    /**
     * 获取字段的上下文配置
     * 查找优先级: PROJECT > ISSUE_TYPE > GLOBAL
     */
    public FieldContext getContext(Long fieldId, Long projectId, Long issueTypeId) {
        // 1. 先查找项目级上下文
        FieldContext context = findContext(fieldId, "PROJECT", projectId);
        if (context != null) return context;
        
        // 2. 查找类型级上下文
        context = findContext(fieldId, "ISSUE_TYPE", issueTypeId);
        if (context != null) return context;
        
        // 3. 返回全局默认
        return findContext(fieldId, "GLOBAL", null);
    }
    
    /**
     * 获取字段选项（考虑上下文）
     */
    public List<FieldOption> getOptions(Long fieldId, Long projectId, Long issueTypeId) {
        FieldContext context = getContext(fieldId, projectId, issueTypeId);
        if (context == null) {
            // 返回字段定义中的全局选项
            return fieldDefinition.getGlobalOptions();
        }
        return context.getOptions();
    }
    
    /**
     * 获取字段在屏幕中的配置
     */
    public List<FieldScreenConfig> getScreenFields(Long screenId, String screenType) {
        // 根据screenType获取对应的screen配置
    }
}
```

### 4.2 工作项创建时字段解析

```java
public class WorkItemCreateService {
    
    public WorkItemResponse create(WorkItemCreateRequest request) {
        Long projectId = request.getProjectId();
        Long issueTypeId = request.getIssueTypeId();
        
        // 1. 获取类型对应的screen方案
        ScreenScheme scheme = screenSchemeService.getByIssueType(issueTypeId);
        
        // 2. 获取创建屏幕
        Screen createScreen = scheme.getCreateScreen();
        
        // 3. 获取屏幕字段配置
        List<FieldScreenConfig> screenFields = 
            fieldContextService.getScreenFields(createScreen.getId(), "EDIT");
        
        // 4. 验证必填字段
        for (FieldScreenConfig config : screenFields) {
            if (config.isRequired() && !request.hasField(config.getFieldKey())) {
                throw new RequiredFieldException(config.getFieldKey());
            }
        }
        
        // 5. 获取字段选项（考虑上下文）
        Map<String, List<FieldOption>> fieldOptions = new HashMap<>();
        for (FieldScreenConfig config : screenFields) {
            List<FieldOption> options = fieldContextService.getOptions(
                config.getFieldDefinitionId(), projectId, issueTypeId);
            fieldOptions.put(config.getFieldKey(), options);
        }
        
        // 6. 返回带选项的创建界面配置
        return buildCreateResponse(screenFields, fieldOptions);
    }
}
```

---

## 5. 验收标准

### 5.1 功能验收
- [ ] 支持创建字段上下文（按项目/类型）
- [ ] 支持在上下文中配置选项
- [ ] 支持全局默认选项
- [ ] 上下文优先级: 项目 > 类型 > 全局
- [ ] 支持创建屏幕配置字段位置
- [ ] 支持屏幕方案关联类型

### 5.2 接口验收
- [ ] 创建上下文返回正确配置
- [ ] 获取字段选项考虑上下文
- [ ] 工作项创建时验证必填字段
- [ ] 返回屏幕字段带选项列表

### 5.3 场景���试

**场景1: 不同项目优先级选项不同**
```
项目A: [P1, P2, P3, P4]
项目B: [Blocker, Critical, Major, Minor]

创建工作项时正确显示对应选项
```

**场景2: 字段上下文的继承**
```
类型上下文覆盖项目上下文
全局作为fallback
```

**场景3: 屏幕方案**
```
Task类型: 创建屏幕A, 编辑屏幕B
Bug类型: 创建屏幕C, 编辑屏幕D
根据类型自动使用对应屏幕
```