# 关系管理模块设计文档 (SDD)

## 1. 概述

### 1.1 模块目标
实现工作项之间的关系管理功能，参考Jira的Issue Linking设计，支持多种关系类型（阻塞、依赖、重复等）。

### 1.2 核心功能
- 创建工作项关系
- 查询工作项关系（双向查询）
- 删除工作项关系
- 关系类型管理
- 关系可视化展示

### 1.3 业务场景
- **阻塞关系**：任务A阻塞任务B（Blocks / is blocked by）
- **依赖关系**：任务A依赖于任务B（Relates to / is related by）
- **重复关系**：任务A是任务B的重复（Duplicates / is duplicated by）
- **父子关系**：任务A是任务B的子任务（is parent of / is child of）

## 2. 数据库设计

### 2.1 关系类型定义表 (issue_link_type)

```sql
DROP TABLE IF EXISTS issue_link_type CASCADE;
CREATE TABLE issue_link_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    link_key VARCHAR(50) NOT NULL,              -- 关系标识: blocks, relates_to, duplicates
    inward_name VARCHAR(100) NOT NULL,          -- 内向名称: "被阻塞"
    outward_name VARCHAR(100) NOT NULL,         -- 外向名称: "阻塞"
    description TEXT,                           -- 描述
    is_system BOOLEAN DEFAULT FALSE,            -- 是否系统类型
    display_order INTEGER DEFAULT 0,            -- 显示顺序
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, link_key)
);

CREATE INDEX idx_linktype_tenant ON issue_link_type(tenant_id);

COMMENT ON TABLE issue_link_type IS '工作项关系类型定义';
COMMENT ON COLUMN issue_link_type.link_key IS '关系标识（唯一）';
COMMENT ON COLUMN issue_link_type.inward_name IS '内向名称（被动语态）';
COMMENT ON COLUMN issue_link_type.outward_name IS '外向名称（主动语态）';
```

### 2.2 工作项关系表 (issue_link)

```sql
DROP TABLE IF EXISTS issue_link CASCADE;
CREATE TABLE issue_link (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    link_type_id BIGINT NOT NULL,               -- 关系类型ID
    source_item_id BIGINT NOT NULL,             -- 源工作项ID（主动方）
    target_item_id BIGINT NOT NULL,             -- 目标工作项ID（被动方）
    comment TEXT,                               -- 关系备注
    created_by BIGINT NOT NULL,                 -- 创建人
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CHECK (source_item_id != target_item_id)    -- 不能自关联
);

CREATE INDEX issuelink_source_idx ON issue_link(source_item_id);
CREATE INDEX issuelink_target_idx ON issue_link(target_item_id);
CREATE INDEX issuelink_type_idx ON issue_link(link_type_id);
CREATE INDEX issuelink_tenant_idx ON issue_link(tenant_id);

COMMENT ON TABLE issue_link IS '工作项关系实例';
COMMENT ON COLUMN issue_link.source_item_id IS '源工作项（主动方）';
COMMENT ON COLUMN issue_link.target_item_id IS '目标工作项（被动方）';
```

### 2.3 初始数据

```sql
-- 系统预定义关系类型
INSERT INTO issue_link_type (tenant_id, link_key, inward_name, outward_name, description, is_system, display_order) VALUES
(1, 'blocks', '被阻塞', '阻塞', '阻止另一个工作项的进行', TRUE, 1),
(1, 'relates_to', '相关于', '关联到', '与另一个工作项相关', TRUE, 2),
(1, 'duplicates', '被重复', '重复', '是另一个工作项的重复', TRUE, 3),
(1, 'clones', '被克隆', '克隆', '克隆自另一个工作项', TRUE, 4)
ON CONFLICT (tenant_id, link_key) DO NOTHING;
```

## 3. 实体类设计

### 3.1 IssueLinkType 实体

```java
@Data
@TableName("issue_link_type")
public class IssueLinkType {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private String linkKey;           // blocks, relates_to, duplicates
    private String inwardName;        // 被阻塞
    private String outwardName;       // 阻塞
    private String description;
    private Boolean isSystem;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

### 3.2 IssueLink 实体

```java
@Data
@TableName("issue_link")
public class IssueLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private Long linkTypeId;
    private Long sourceItemId;        // 源工作项
    private Long targetItemId;        // 目标工作项
    private String comment;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
```

## 4. DTO设计

### 4.1 IssueLinkTypeResponse

```java
@Data
public class IssueLinkTypeResponse {
    private Long id;
    private String linkKey;
    private String inwardName;
    private String outwardName;
    private String description;
    private Boolean isSystem;
    private Integer displayOrder;
}
```

### 4.2 IssueLinkCreateRequest

```java
@Data
public class IssueLinkCreateRequest {
    @NotBlank(message = "关系类型不能为空")
    private String linkTypeKey;       // blocks, relates_to
    
    @NotNull(message = "源工作项ID不能为空")
    private Long sourceItemId;
    
    @NotNull(message = "目标工作项ID不能为空")
    private Long targetItemId;
    
    private String comment;
}
```

### 4.3 IssueLinkResponse

```java
@Data
public class IssueLinkResponse {
    private Long id;
    private String linkTypeKey;
    private String linkTypeName;      // 显示名称：阻塞/被阻塞
    private String direction;         // inward/outward
    private Long relatedItemId;       // 关联的工作项ID
    private String relatedItemKey;    // 关联的工作项编号
    private String relatedItemSummary;// 关联的工作项摘要
    private String comment;
    private Long createdBy;
    private LocalDateTime createdAt;
}
```

## 5. API设计

### 5.1 关系类型管理

```
GET    /api/v1/link-types                  # 获取所有关系类型
GET    /api/v1/link-types/{id}             # 获取单个关系类型
POST   /api/v1/link-types                  # 创建关系类型
PUT    /api/v1/link-types/{id}             # 更新关系类型
DELETE /api/v1/link-types/{id}             # 删除关系类型
```

### 5.2 工作项关系管理

```
GET    /api/v1/work-items/{itemId}/links   # 获取工作项的所有关系
POST   /api/v1/work-items/{itemId}/links   # 创建工作项关系
DELETE /api/v1/work-items/links/{linkId}   # 删除工作项关系
```

## 6. Service层设计

### 6.1 IssueLinkTypeService

```java
@Service
public class IssueLinkTypeService extends ServiceImpl<IssueLinkTypeMapper, IssueLinkType> {
    // 获取所有关系类型
    List<IssueLinkTypeResponse> listAll(Long tenantId);
    
    // 创建关系类型
    IssueLinkTypeResponse create(Long tenantId, IssueLinkTypeCreateRequest request);
    
    // 更新关系类型
    IssueLinkTypeResponse update(Long id, IssueLinkTypeUpdateRequest request);
    
    // 删除关系类型（系统类型不允许删除）
    void delete(Long id);
}
```

### 6.2 IssueLinkService

```java
@Service
public class IssueLinkService extends ServiceImpl<IssueLinkMapper, IssueLink> {
    // 获取工作项的所有关系（双向）
    List<IssueLinkResponse> getLinksByItemId(Long tenantId, Long itemId);
    
    // 创建工作项关系
    IssueLinkResponse create(Long tenantId, Long userId, IssueLinkCreateRequest request);
    
    // 删除工作项关系
    void delete(Long id);
    
    // 验证关系是否存在
    boolean exists(Long sourceItemId, Long targetItemId, Long linkTypeId);
}
```

## 7. 业务规则

### 7.1 关系创建规则
1. **不能自关联**：sourceItemId != targetItemId
2. **关系类型必须存在**：linkTypeKey必须在数据库中
3. **工作项必须存在**：sourceItemId和targetItemId必须有效
4. **租户隔离**：所有操作基于tenantId
5. **防重复**：同一对工项不能有相同类型的关系

### 7.2 关系查询规则
1. **双向查询**：
   - 作为source时，显示outwardName（阻塞）
   - 作为target时，显示inwardName（被阻塞）
2. **按类型分组**：返回结果按linkType分组
3. **包含工作项信息**：返回关联工作项的key和summary

### 7.3 删除规则
1. **系统类型保护**：is_system=true的类型不允许删除
2. **级联检查**：删除关系类型前检查是否有使用该类型的关系

## 8. 前端页面设计

### 8.1 关系类型配置页面
路径：`/config/link-types`

功能：
- 关系类型列表展示
- 新增/编辑/删除关系类型
- 系统类型标记（不可删除）

### 8.2 工作项关系管理
在工作项详情页添加"关系"标签页

功能：
- 显示所有关联的工作项（按类型分组）
- 添加新关系（选择类型+选择工作项）
- 删除关系
- 显示关系方向（阻塞/被阻塞）

## 9. 开发计划

### Phase 1: 数据库和实体层
- [ ] 执行数据库脚本
- [ ] 创建实体类
- [ ] 创建Mapper接口

### Phase 2: Service层
- [ ] IssueLinkTypeService实现
- [ ] IssueLinkService实现
- [ ] 单元测试

### Phase 3: Controller层
- [ ] IssueLinkTypeController
- [ ] IssueLinkController
- [ ] API测试

### Phase 4: 前端开发
- [ ] 关系类型配置页面
- [ ] 工作项关系管理组件
- [ ] 集成到工作项详情页

### Phase 5: 测试和优化
- [ ] 功能测试
- [ ] 性能优化
- [ ] 用户体验优化

## 10. 技术要点

### 10.1 双向关系处理
```java
// 查询示例：获取工作项123的所有关系
SELECT 
    il.id,
    ilt.link_key,
    CASE 
        WHEN il.source_item_id = 123 THEN ilt.outward_name
        ELSE ilt.inward_name
    END as link_direction,
    CASE 
        WHEN il.source_item_id = 123 THEN il.target_item_id
        ELSE il.source_item_id
    END as related_item_id
FROM issue_link il
JOIN issue_link_type ilt ON il.link_type_id = ilt.id
WHERE (il.source_item_id = 123 OR il.target_item_id = 123)
  AND il.deleted = false;
```

### 10.2 防重复机制
```java
// 在创建前检查
LambdaQueryWrapper<IssueLink> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(IssueLink::getSourceItemId, sourceItemId)
       .eq(IssueLink::getTargetItemId, targetItemId)
       .eq(IssueLink::getLinkTypeId, linkTypeId);
       
if (this.count(wrapper) > 0) {
    throw new RuntimeException("关系已存在");
}
```

## 11. 扩展考虑

### 11.1 未来扩展
- 关系历史记录
- 关系权限控制
- 关系可视化图表
- 批量操作支持

### 11.2 性能优化
- 添加合适的索引
- 缓存关系类型
- 分页查询大量关系
