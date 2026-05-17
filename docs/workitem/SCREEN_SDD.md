# 工作项Screen功能管理模块 - SDD设计文档

## 1. 概述

### 1.1 模块目标
实现类似Jira的Screen（屏幕）架构，用于管理工作项在不同操作场景下显示的字段集合及其布局。通过Screen机制，可以灵活配置不同问题类型、不同操作（创建/编辑/查看/转换）时显示的字段。

### 1.2 核心概念
- **Screen（屏幕）**：字段的集合，定义在特定场景下显示哪些字段
- **Screen Tab（屏幕标签页）**：将字段分组到不同的标签页中，优化用户体验
- **Screen Item（屏幕项）**：Screen与字段的关联关系，包含字段在Screen中的位置信息
- **Screen Scheme（屏幕方案）**：将Screen映射到不同的操作类型（Create/Edit/View/Transition）
- **Issue Type Screen Scheme（问题类型屏幕方案）**：将Screen Scheme映射到不同的问题类型

### 1.3 业务价值
1. **灵活的字段展示**：根据不同场景显示不同字段
2. **简化用户操作**：创建时只显示必要字段，减少认知负担
3. **逻辑分组**：通过Tab页组织字段，提升可读性
4. **多租户隔离**：支持不同租户自定义Screen配置

## 2. Jira Screen架构分析

### 2.1 Jira Screen核心组件

| 组件 | 说明 | 作用 |
|------|------|------|
| Field | 字段定义 | 数据收集的基本单元 |
| Screen | 屏幕 | 字段的容器，定义显示哪些字段 |
| Screen Tab | 屏幕标签页 | 将字段分组到不同Tab |
| Screen Item | 屏幕项 | Screen与Field的关联关系 |
| Screen Scheme | 屏幕方案 | 将Screen映射到操作（Create/Edit/View） |
| Issue Type Screen Scheme | 问题类型屏幕方案 | 将Screen Scheme映射到Issue Type |

### 2.2 Jira Screen工作流程

```
Issue Type → Issue Type Screen Scheme → Screen Scheme → Screen → Screen Tab → Screen Item → Field
```

**示例流程：**
1. 用户创建"Bug"类型的问题
2. 系统查找Bug对应的Issue Type Screen Scheme
3. 找到Create操作对应的Screen Scheme
4. 获取Create Screen
5. 渲染Screen中的所有Tab和字段

### 2.3 本系统与Jira的对比

| 特性 | Jira | 本系统 | 说明 |
|------|------|--------|------|
| Screen管理 | ✅ | ✅ | 创建/编辑/删除Screen |
| Screen Tab | ✅ | ✅ | 支持多Tab组织字段 |
| Screen Item | ✅ | ✅ | 字段与Screen的关联 |
| Screen Scheme | ✅ | ⚠️简化 | 直接关联到Issue Type |
| Issue Type Screen Scheme | ✅ | ⚠️简化 | 合并到Screen配置 |
| Transition Screen | ✅ | ❌二期 | 工作流转换屏幕 |
| 字段顺序 | ✅ | ✅ | 支持拖拽排序 |
| 字段必填控制 | ✅ | ❌ | 由Field Configuration控制 |
| 条件显示 | ✅ | ❌二期 | 根据条件显示/隐藏字段 |

**简化策略：**
- 将Screen Scheme和Issue Type Screen Scheme合并，直接在Screen上关联Issue Type和操作类型
- 暂不实现Transition Screen（工作流转换屏幕），在二期实现
- 字段必填控制由Field Configuration统一管理

## 3. 数据库设计

### 3.1 表结构设计

#### 3.1.1 screen（屏幕表）

```sql
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    screen_name VARCHAR(100) NOT NULL,          -- 屏幕名称
    description TEXT,                            -- 描述
    is_system BOOLEAN DEFAULT FALSE,             -- 是否系统内置
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, screen_name)
);

CREATE INDEX idx_screen_tenant ON screen(tenant_id);

COMMENT ON TABLE screen IS '屏幕定义表';
COMMENT ON COLUMN screen.screen_name IS '屏幕名称';
COMMENT ON COLUMN screen.is_system IS '系统内置屏幕不可删除';
```

#### 3.1.2 screen_tab（屏幕标签页表）

```sql
CREATE TABLE screen_tab (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    tab_name VARCHAR(100) NOT NULL,              -- 标签页名称
    display_order INTEGER DEFAULT 0,             -- 显示顺序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_screentab_screen ON screen_tab(screen_id);

COMMENT ON TABLE screen_tab IS '屏幕标签页表';
COMMENT ON COLUMN screen_tab.tab_name IS '标签页名称，如：详情、人员、日期';
```

#### 3.1.3 screen_item（屏幕项表）

```sql
CREATE TABLE screen_item (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    screen_tab_id BIGINT REFERENCES screen_tab(id), -- 可为空，表示默认Tab
    field_definition_id BIGINT NOT NULL REFERENCES field_definition(id),
    display_order INTEGER DEFAULT 0,             -- 在Tab中的显示顺序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(screen_id, field_definition_id)
);

CREATE INDEX idx_screenitem_screen ON screen_item(screen_id);
CREATE INDEX idx_screenitem_tab ON screen_item(screen_tab_id);
CREATE INDEX idx_screenitem_field ON screen_item(field_definition_id);

COMMENT ON TABLE screen_item IS '屏幕项表（Screen与Field的关联）';
COMMENT ON COLUMN screen_item.screen_tab_id IS '所属标签页，为空表示默认Tab';
```

#### 3.1.4 issue_type_screen（问题类型屏幕关联表）

```sql
CREATE TABLE issue_type_screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    work_item_type_id BIGINT NOT NULL REFERENCES work_item_type(id),
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    operation_type VARCHAR(20) NOT NULL,         -- CREATE/EDIT/VIEW
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, work_item_type_id, operation_type)
);

CREATE INDEX idx_its_tenant ON issue_type_screen(tenant_id);
CREATE INDEX idx_its_type ON issue_type_screen(work_item_type_id);
CREATE INDEX idx_its_screen ON issue_type_screen(screen_id);

COMMENT ON TABLE issue_type_screen IS '问题类型与屏幕的关联表';
COMMENT ON COLUMN issue_type_screen.operation_type IS '操作类型：CREATE/EDIT/VIEW';
```

### 3.2 初始数据

```sql
-- 系统默认Screen
INSERT INTO screen (tenant_id, screen_name, description, is_system) VALUES
(1, 'Default Screen', '默认屏幕', TRUE)
ON CONFLICT (tenant_id, screen_name) DO NOTHING;

-- 默认Tab
INSERT INTO screen_tab (screen_id, tab_name, display_order)
SELECT id, '详情', 0 FROM screen WHERE screen_name = 'Default Screen' AND tenant_id = 1
ON CONFLICT DO NOTHING;

-- 将所有系统字段添加到默认Screen
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Default Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND fd.is_system = TRUE
  AND fd.tenant_id = 1
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 关联所有Issue Type到默认Screen
INSERT INTO issue_type_screen (tenant_id, work_item_type_id, screen_id, operation_type)
SELECT 
    1,
    wit.id,
    s.id,
    op.operation
FROM work_item_type wit
CROSS JOIN screen s
CROSS JOIN (VALUES ('CREATE'), ('EDIT'), ('VIEW')) AS op(operation)
WHERE wit.tenant_id = 1
  AND s.screen_name = 'Default Screen'
  AND s.tenant_id = 1
ON CONFLICT (tenant_id, work_item_type_id, operation_type) DO NOTHING;
```

## 4. 实体类设计

### 4.1 Screen实体

```java
@Data
@TableName("screen")
public class Screen {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private String screenName;
    private String description;
    private Boolean isSystem;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

### 4.2 ScreenTab实体

```java
@Data
@TableName("screen_tab")
public class ScreenTab {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long screenId;
    private String tabName;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

### 4.3 ScreenItem实体

```java
@Data
@TableName("screen_item")
public class ScreenItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long screenId;
    private Long screenTabId;  // 可为空
    private Long fieldDefinitionId;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

### 4.4 IssueTypeScreen实体

```java
@Data
@TableName("issue_type_screen")
public class IssueTypeScreen {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private Long workItemTypeId;
    private Long screenId;
    private String operationType;  // CREATE/EDIT/VIEW
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

## 5. DTO设计

### 5.1 ScreenResponse

```java
@Data
public class ScreenResponse {
    private Long id;
    private String screenName;
    private String description;
    private Boolean isSystem;
    private List<ScreenTabResponse> tabs;  // 包含的Tab
    private Map<String, String> issueTypeMappings;  // Issue Type映射
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### 5.2 ScreenTabResponse

```java
@Data
public class ScreenTabResponse {
    private Long id;
    private String tabName;
    private Integer displayOrder;
    private List<ScreenItemResponse> items;  // Tab中的字段
}
```

### 5.3 ScreenItemResponse

```java
@Data
public class ScreenItemResponse {
    private Long id;
    private Long fieldDefinitionId;
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private Integer displayOrder;
}
```

### 5.4 ScreenCreateRequest

```java
@Data
public class ScreenCreateRequest {
    @NotBlank(message = "屏幕名称不能为空")
    private String screenName;
    
    private String description;
    
    private List<Long> fieldIds;  // 初始字段列表
}
```

### 5.5 ScreenUpdateRequest

```java
@Data
public class ScreenUpdateRequest {
    private String screenName;
    private String description;
}
```

### 5.6 AddFieldToScreenRequest

```java
@Data
public class AddFieldToScreenRequest {
    @NotNull(message = "字段ID不能为空")
    private Long fieldDefinitionId;
    
    private Long screenTabId;  // 可选，不填则添加到默认Tab
    
    private Integer displayOrder;
}
```

### 5.7 IssueTypeScreenMappingRequest

```java
@Data
public class IssueTypeScreenMappingRequest {
    @NotNull(message = "问题类型ID不能为空")
    private Long workItemTypeId;
    
    @NotBlank(message = "操作类型不能为空")
    private String operationType;  // CREATE/EDIT/VIEW
    
    @NotNull(message = "屏幕ID不能为空")
    private Long screenId;
}
```

## 6. API设计

### 6.1 Screen管理API

```
GET    /api/v1/screens                          # 获取所有Screen
GET    /api/v1/screens/{id}                     # 获取Screen详情（含Tab和字段）
POST   /api/v1/screens                          # 创建Screen
PUT    /api/v1/screens/{id}                     # 更新Screen
DELETE /api/v1/screens/{id}                     # 删除Screen（系统Screen不可删）
```

### 6.2 Screen Tab管理API

```
POST   /api/v1/screens/{screenId}/tabs          # 添加Tab
PUT    /api/v1/screens/tabs/{tabId}             # 更新Tab
DELETE /api/v1/screens/tabs/{tabId}             # 删除Tab
PUT    /api/v1/screens/{screenId}/tabs/reorder  # 调整Tab顺序
```

### 6.3 Screen Item管理API

```
POST   /api/v1/screens/{screenId}/items         # 添加字段到Screen
DELETE /api/v1/screens/items/{itemId}           # 从Screen移除字段
PUT    /api/v1/screens/{screenId}/items/reorder # 调整字段顺序
```

### 6.4 Issue Type Screen映射API

```
GET    /api/v1/issue-type-screens               # 获取所有映射
POST   /api/v1/issue-type-screens               # 创建映射
PUT    /api/v1/issue-type-screens/{id}          # 更新映射
DELETE /api/v1/issue-type-screens/{id}          # 删除映射
GET    /api/v1/issue-types/{typeId}/screens     # 获取某Issue Type的所有Screen
```

## 7. Service层设计

### 7.1 ScreenService

```java
@Service
public class ScreenService extends ServiceImpl<ScreenMapper, Screen> {
    // 获取所有Screen（含Tab和字段）
    List<ScreenResponse> listAll(Long tenantId);
    
    // 获取Screen详情
    ScreenResponse getById(Long id);
    
    // 创建Screen
    ScreenResponse create(Long tenantId, ScreenCreateRequest request);
    
    // 更新Screen
    ScreenResponse update(Long id, ScreenUpdateRequest request);
    
    // 删除Screen（系统Screen不可删）
    void delete(Long id);
    
    // 添加Tab
    ScreenTabResponse addTab(Long screenId, String tabName);
    
    // 添加字段到Screen
    ScreenItemResponse addField(Long screenId, AddFieldToScreenRequest request);
    
    // 从Screen移除字段
    void removeField(Long itemId);
}
```

### 7.2 IssueTypeScreenService

```java
@Service
public class IssueTypeScreenService extends ServiceImpl<IssueTypeScreenMapper, IssueTypeScreen> {
    // 获取Issue Type的Screen
    ScreenResponse getScreenForIssueType(Long tenantId, Long issueTypeId, String operationType);
    
    // 创建映射
    void createMapping(IssueTypeScreenMappingRequest request);
    
    // 更新映射
    void updateMapping(Long id, IssueTypeScreenMappingRequest request);
    
    // 删除映射
    void deleteMapping(Long id);
}
```

## 8. 业务规则

### 8.1 Screen创建规则
1. **名称唯一性**：同一租户下Screen名称不能重复
2. **默认Tab**：创建Screen时自动创建一个默认Tab（"详情"）
3. **系统字段**：系统Screen不可删除

### 8.2 字段添加规则
1. **防重复**：同一字段不能在同一个Screen中出现多次
2. **Tab归属**：字段必须属于某个Tab（可以是默认Tab）
3. **顺序控制**：支持手动指定displayOrder

### 8.3 映射规则
1. **唯一性**：同一Issue Type + Operation只能映射一个Screen
2. **默认Screen**：未配置时使用Default Screen
3. **操作类型**：仅支持CREATE/EDIT/VIEW三种

### 8.4 删除保护
1. **系统Screen**：is_system=true的Screen不可删除
2. **使用中检查**：被Issue Type引用的Screen删除时需警告

## 9. 前端页面设计

### 9.1 Screen列表页面
路径：`/config/screens`

功能：
- Screen列表展示（名称、描述、是否系统、关联的Issue Type数量）
- 新建Screen按钮
- 编辑/删除操作（系统Screen不可删）
- 点击进入Screen详情

### 9.2 Screen详情页面
路径：`/config/screens/{id}`

功能：
- **基本信息区**：Screen名称、描述、编辑按钮
- **Tab管理区**：
  - Tab列表（可拖拽排序）
  - 新增Tab按钮
  - 重命名/删除Tab
- **字段管理区**（当前选中Tab）：
  - 字段列表（字段名、类型、顺序）
  - 添加字段按钮（从可用字段池选择）
  - 拖拽调整字段顺序
  - 移除字段按钮
- **Issue Type映射区**：
  - 显示哪些Issue Type使用了此Screen
  - 为不同操作（Create/Edit/View）配置Screen

### 9.3 Issue Type Screen配置页面
路径：`/config/issue-type-screens`

功能：
- 按Issue Type分组展示
- 每个Issue Type显示三个操作的Screen配置
- 下拉选择Screen进行配置
- 实时预览效果

## 10. 开发计划

### Phase 1: 数据库和实体层（1天）
- [ ] 执行数据库脚本
- [ ] 创建实体类（Screen/ScreenTab/ScreenItem/IssueTypeScreen）
- [ ] 创建Mapper接口
- [ ] 初始化默认数据

### Phase 2: Service层（2天）
- [ ] ScreenService实现（CRUD + Tab管理 + 字段管理）
- [ ] IssueTypeScreenService实现
- [ ] 业务规则验证
- [ ] 单元测试

### Phase 3: Controller层（1天）
- [ ] ScreenController
- [ ] IssueTypeScreenController
- [ ] API测试

### Phase 4: 前端开发（3天）
- [ ] Screen列表页面
- [ ] Screen详情页面（Tab管理 + 字段管理）
- [ ] Issue Type Screen配置页面
- [ ] 拖拽排序功能
- [ ] 字段选择器组件

### Phase 5: 集成测试和优化（1天）
- [ ] 端到端测试
- [ ] 性能优化
- [ ] 用户体验优化

## 11. 技术要点

### 11.1 拖拽排序实现
```javascript
// 使用Sortable.js或Vue Draggable
import draggable from 'vuedraggable'

<draggable v-model="tabList" @end="onTabReorder">
  <div v-for="tab in tabList" :key="tab.id">{{ tab.tabName }}</div>
</draggable>
```

### 11.2 字段选择器
```vue
<el-select v-model="selectedFieldId" filterable>
  <el-option
    v-for="field in availableFields"
    :key="field.id"
    :label="field.fieldName"
    :value="field.id"
  />
</el-select>
```

### 11.3 级联查询优化
```java
// 一次性查询Screen及其Tab和字段
@Select("""
    SELECT s.*, st.*, si.*, fd.*
    FROM screen s
    LEFT JOIN screen_tab st ON s.id = st.screen_id
    LEFT JOIN screen_item si ON st.id = si.screen_tab_id
    LEFT JOIN field_definition fd ON si.field_definition_id = fd.id
    WHERE s.id = #{id} AND s.deleted = false
    ORDER BY st.display_order, si.display_order
""")
ScreenDetailVO getScreenDetail(Long id);
```

## 12. 扩展考虑

### 12.1 未来扩展
- **Transition Screen**：工作流转换时的屏幕
- **条件显示**：根据字段值动态显示/隐藏其他字段
- **字段上下文**：同一字段在不同Screen中有不同配置
- **Screen模板**：快速复制Screen配置
- **权限控制**：不同角色看到不同的Screen

### 12.2 性能优化
- **缓存Screen配置**：Redis缓存常用Screen
- **懒加载**：详情页按需加载Tab和字段
- **批量操作**：支持批量添加/移除字段

## 13. 验收标准

### 13.1 功能验收
- [ ] 可以创建/编辑/删除Screen
- [ ] 可以为Screen添加/删除Tab
- [ ] 可以为Tab添加/移除字段
- [ ] 支持拖拽调整Tab和字段顺序
- [ ] 可以为Issue Type配置不同操作的Screen
- [ ] 系统Screen不可删除
- [ ] 字段不能重复添加到同一Screen

### 13.2 技术验收
- [ ] 代码编译通过
- [ ] 单元测试覆盖率>70%
- [ ] API符合RESTful规范
- [ ] 前端响应式布局
- [ ] 错误处理完善

## 14. 参考资料

- [Jira Screens Documentation](https://confluence.atlassian.com/adminjiraserver/configuring-screens-938847326.html)
- [Jira Screen Schemes](https://confluence.atlassian.com/adminjiraserver/configuring-screen-schemes-938847328.html)
- RELATION_DESIGN.md - 关系管理模块设计参考
