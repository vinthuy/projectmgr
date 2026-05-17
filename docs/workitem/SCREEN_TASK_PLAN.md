# Screen功能开发任务规划

## 项目概览
基于SDD设计文档，实现类似Jira的Screen架构，管理工作项字段展示配置。

## 任务分解

### Phase 1: 数据库和实体层 (预计1天)

#### Task 1.1: 数据库脚本
- [ ] 创建screen表
- [ ] 创建screen_tab表
- [ ] 创建screen_item表
- [ ] 创建issue_type_screen表
- [ ] 添加索引和约束
- [ ] 插入初始数据（Default Screen）

**文件**: `src/main/resources/schema.sql`

#### Task 1.2: 实体类创建
- [ ] Screen.java
- [ ] ScreenTab.java
- [ ] ScreenItem.java
- [ ] IssueTypeScreen.java

**文件**: `src/main/java/com/workitem/entity/`

#### Task 1.3: Mapper接口
- [ ] ScreenMapper.java
- [ ] ScreenTabMapper.java
- [ ] ScreenItemMapper.java
- [ ] IssueTypeScreenMapper.java

**文件**: `src/main/java/com/workitem/mapper/`

---

### Phase 2: DTO层 (预计0.5天)

#### Task 2.1: Request DTOs
- [ ] ScreenCreateRequest.java
- [ ] ScreenUpdateRequest.java
- [ ] AddFieldToScreenRequest.java
- [ ] IssueTypeScreenMappingRequest.java

**文件**: `src/main/java/com/workitem/dto/`

#### Task 2.2: Response DTOs
- [ ] ScreenResponse.java
- [ ] ScreenTabResponse.java
- [ ] ScreenItemResponse.java
- [ ] IssueTypeScreenResponse.java

**文件**: `src/main/java/com/workitem/dto/`

---

### Phase 3: Service层 (预计2天)

#### Task 3.1: ScreenService
- [ ] listAll() - 获取所有Screen
- [ ] getById() - 获取Screen详情（含Tab和字段）
- [ ] create() - 创建Screen（自动创建默认Tab）
- [ ] update() - 更新Screen基本信息
- [ ] delete() - 删除Screen（系统Screen保护）
- [ ] addTab() - 添加Tab
- [ ] updateTab() - 更新Tab
- [ ] deleteTab() - 删除Tab
- [ ] reorderTabs() - 调整Tab顺序
- [ ] addField() - 添加字段到Screen
- [ ] removeField() - 从Screen移除字段
- [ ] reorderFields() - 调整字段顺序

**文件**: `src/main/java/com/workitem/service/ScreenService.java`

#### Task 3.2: IssueTypeScreenService
- [ ] getScreenForIssueType() - 获取Issue Type的Screen
- [ ] listMappings() - 获取所有映射
- [ ] createMapping() - 创建映射
- [ ] updateMapping() - 更新映射
- [ ] deleteMapping() - 删除映射
- [ ] validateMapping() - 验证映射唯一性

**文件**: `src/main/java/com/workitem/service/IssueTypeScreenService.java`

#### Task 3.3: 单元测试
- [ ] ScreenServiceTest.java
- [ ] IssueTypeScreenServiceTest.java

**文件**: `src/test/java/com/workitem/service/`

---

### Phase 4: Controller层 (预计1天)

#### Task 4.1: ScreenController
- [ ] GET /api/v1/screens
- [ ] GET /api/v1/screens/{id}
- [ ] POST /api/v1/screens
- [ ] PUT /api/v1/screens/{id}
- [ ] DELETE /api/v1/screens/{id}
- [ ] POST /api/v1/screens/{screenId}/tabs
- [ ] PUT /api/v1/screens/tabs/{tabId}
- [ ] DELETE /api/v1/screens/tabs/{tabId}
- [ ] PUT /api/v1/screens/{screenId}/tabs/reorder
- [ ] POST /api/v1/screens/{screenId}/items
- [ ] DELETE /api/v1/screens/items/{itemId}
- [ ] PUT /api/v1/screens/{screenId}/items/reorder

**文件**: `src/main/java/com/workitem/controller/ScreenController.java`

#### Task 4.2: IssueTypeScreenController
- [ ] GET /api/v1/issue-type-screens
- [ ] POST /api/v1/issue-type-screens
- [ ] PUT /api/v1/issue-type-screens/{id}
- [ ] DELETE /api/v1/issue-type-screens/{id}
- [ ] GET /api/v1/issue-types/{typeId}/screens

**文件**: `src/main/java/com/workitem/controller/IssueTypeScreenController.java`

---

### Phase 5: 前端API层 (预计0.5天)

#### Task 5.1: screen.js
- [ ] getScreens()
- [ ] getScreenById()
- [ ] createScreen()
- [ ] updateScreen()
- [ ] deleteScreen()
- [ ] addTab()
- [ ] updateTab()
- [ ] deleteTab()
- [ ] reorderTabs()
- [ ] addFieldToScreen()
- [ ] removeFieldFromScreen()
- [ ] reorderFields()

**文件**: `frontend/src/api/screen.js`

#### Task 5.2: issueTypeScreen.js
- [ ] getIssueTypeScreens()
- [ ] createMapping()
- [ ] updateMapping()
- [ ] deleteMapping()
- [ ] getScreensForIssueType()

**文件**: `frontend/src/api/issueTypeScreen.js`

---

### Phase 6: 前端页面开发 (预计3天)

#### Task 6.1: Screen列表页面
- [ ] ScreenList.vue - 列表展示
- [ ] 新建Screen对话框
- [ ] 编辑/删除操作
- [ ] 路由配置

**文件**: `frontend/src/views/config/ScreenList.vue`

#### Task 6.2: Screen详情页面
- [ ] ScreenDetail.vue - 主页面
- [ ] Tab管理组件（可拖拽排序）
- [ ] 字段管理组件（可拖拽排序）
- [ ] 字段选择器组件
- [ ] Issue Type映射配置区
- [ ] 路由配置

**文件**: 
- `frontend/src/views/config/ScreenDetail.vue`
- `frontend/src/components/screen/TabManager.vue`
- `frontend/src/components/screen/FieldManager.vue`
- `frontend/src/components/screen/FieldSelector.vue`

#### Task 6.3: Issue Type Screen配置页面
- [ ] IssueTypeScreenConfig.vue - 配置页面
- [ ] 按Issue Type分组展示
- [ ] Screen下拉选择器
- [ ] 实时保存

**文件**: `frontend/src/views/config/IssueTypeScreenConfig.vue`

#### Task 6.4: 菜单集成
- [ ] 更新Layout.vue添加菜单项
- [ ] 更新router/index.js添加路由

---

### Phase 7: 测试和优化 (预计1天)

#### Task 7.1: 集成测试
- [ ] 端到端流程测试
- [ ] API联调测试
- [ ] 边界条件测试

#### Task 7.2: 性能优化
- [ ] 添加数据库索引
- [ ] 优化查询语句
- [ ] 前端懒加载

#### Task 7.3: 用户体验优化
- [ ] 加载状态提示
- [ ] 错误处理友好提示
- [ ] 操作确认对话框
- [ ] 拖拽视觉反馈

---

## 开发顺序建议

1. **先完成后端核心功能**（Phase 1-4）
   - 确保API可以正常工作
   - 使用Postman或curl测试

2. **再开发前端基础页面**（Phase 5-6.1）
   - 先实现Screen列表和基本的CRUD

3. **逐步完善高级功能**（Phase 6.2-6.3）
   - Tab管理
   - 字段管理
   - 拖拽排序
   - Issue Type映射

4. **最后进行测试和优化**（Phase 7）

---

## 关键技术点

### 后端
- MyBatis Plus级联查询
- 事务管理（@Transactional）
- 业务规则验证
- 唯一性约束检查

### 前端
- Vue 3 Composition API
- Element Plus组件库
- vuedraggable拖拽库
- 响应式数据绑定
- 组件通信（props/emit）

---

## 风险和挑战

1. **拖拽排序复杂性**
   - 解决：使用成熟的vuedraggable库
   
2. **级联数据查询性能**
   - 解决：优化SQL，使用JOIN一次性查询
   
3. **前后端数据结构对齐**
   - 解决：先定义DTO，确保一致性
   
4. **系统Screen保护**
   - 解决：后端强制校验 + 前端禁用操作

---

## 验收清单

### 功能验收
- [ ] 可以创建Screen
- [ ] 可以为Screen添加Tab
- [ ] 可以为Tab添加字段
- [ ] 支持拖拽排序Tab和字段
- [ ] 可以配置Issue Type的Screen
- [ ] 系统Screen不可删除
- [ ] 字段不能重复添加

### 技术验收
- [ ] 代码编译通过
- [ ] 单元测试通过
- [ ] API测试通过
- [ ] 前端页面无报错
- [ ] 浏览器兼容性良好

---

## 时间估算

| Phase | 任务 | 预计时间 |
|-------|------|----------|
| Phase 1 | 数据库和实体层 | 1天 |
| Phase 2 | DTO层 | 0.5天 |
| Phase 3 | Service层 | 2天 |
| Phase 4 | Controller层 | 1天 |
| Phase 5 | 前端API层 | 0.5天 |
| Phase 6 | 前端页面 | 3天 |
| Phase 7 | 测试优化 | 1天 |
| **总计** | | **9天** |

---

## 下一步行动

1. ✅ SDD设计文档已完成
2. ⏭️ 开始Phase 1: 数据库脚本和实体类
3. ⏭️ 按照任务清单逐步开发
4. ⏭️ 每完成一个Phase进行代码审查
