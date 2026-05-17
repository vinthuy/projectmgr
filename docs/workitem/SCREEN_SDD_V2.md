# Screen 配置页面详细设计文档（V2 - ONES风格）

## 1. 概述

### 1.1 文档目的
本文档定义 Screen 配置页面的详细技术实现方案，基于 ONES/Jira 的拖拽式字段配置交互模式。

### 1.2 设计目标
- **可视化配置**：所见即所得的字段布局编辑
- **直观拖拽**：左侧资源库 → 右侧画布的拖拽添加
- **灵活排序**：Tab 和字段的自由拖拽排序
- **实时反馈**：操作即时响应，自动保存

### 1.3 参考系统
- **ONES Project**：工作项属性拖拽配置
- **Jira Screen**：字段管理与 Tab 组织
- **飞书项目**：详情页布局配置

---

## 2. 架构设计

### 2.1 组件结构（简化版）
```
ScreenDetail.vue (单一组件，内联所有逻辑)
├── 顶部操作栏（内联模板）
├── 左侧字段资源库（内联模板）
│   ├── 搜索框
│   └── 字段列表（系统/自定义分组）
└── 右侧画布区（内联模板）
    ├── Tab 卡片列表（可拖拽）
    │   └── 字段列表（可拖拽）
    └── 新建 Tab 按钮
```

**设计理由**：
- 当前功能集中在单页面，无需组件拆分
- 减少 props/emits 传递复杂度
- 便于状态管理和调试
- 后续如需复用再提取组件

### 2.2 数据流（简化版）
```
┌─────────────┐     ┌──────────────┐     ┌──────────────────┐
│   Backend   │←──→│  API Layer   │←──→│ ScreenDetail.vue  │
│  (PostgreSQL)│    │  (screen.js) │    │ (Composition API) │
└─────────────┘     └──────────────┘     └──────────────────┘
                                              ↓
                                        本地响应式状态
                                        (ref/reactive)
```

---

## 3. 数据库设计（复用现有）

### 3.1 表结构
已存在的表结构满足需求，无需修改：

```sql
-- Screen 表
screen (id, tenant_id, screen_name, description, is_system, ...)

-- Tab 表
screen_tab (id, screen_id, tab_name, display_order, ...)

-- 字段关联表
screen_item (id, screen_id, screen_tab_id, field_definition_id, display_order, ...)

-- Issue Type 映射表
issue_type_screen (id, tenant_id, work_item_type_id, screen_id, operation_type, ...)
```

### 3.2 索引优化
```sql
-- 加速 Tab 查询
CREATE INDEX idx_screen_tab_screen_order ON screen_tab(screen_id, display_order);

-- 加速字段查询
CREATE INDEX idx_screen_item_tab_order ON screen_item(screen_tab_id, display_order);
```

---

## 4. API 设计

### 4.1 现有 API（复用）
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/screens` | 获取 Screen 列表 |
| GET | `/api/v1/screens/:id` | 获取 Screen 详情（含 Tabs + Fields） |
| POST | `/api/v1/screens` | 创建 Screen |
| PUT | `/api/v1/screens/:id` | 更新 Screen 基本信息 |
| DELETE | `/api/v1/screens/:id` | 删除 Screen |

### 4.2 Tab 管理 API（优化）
| 方法 | 路径 | 请求体 | 说明 |
|------|------|--------|------|
| POST | `/api/v1/screens/:screenId/tabs` | `{ tabName: string }` | 添加 Tab |
| DELETE | `/api/v1/screens/tabs/:tabId` | - | 删除 Tab |
| PUT | `/api/v1/screens/:screenId/tabs/reorder` | `tabIds: number[]` | 调整 Tab 顺序 |

**优化说明**：
- 移除 `PUT /tabs/:tabId`，Tab 重命名功能暂不实现（双击编辑在本地完成，保存时一起提交）
- 简化 API 数量，降低维护成本

### 4.3 字段管理 API
| 方法 | 路径 | 请求体 | 说明 |
|------|------|--------|------|
| POST | `/api/v1/screens/:screenId/items` | `{ fieldDefinitionId, screenTabId }` | 添加字段到 Tab |
| DELETE | `/api/v1/screens/items/:itemId` | - | 从 Screen 移除字段 |
| PUT | `/api/v1/screens/:screenId/items/reorder` | `itemIds: number[]` | 调整字段顺序 |

### 4.4 字段资源 API
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/field-definitions` | 获取可用字段列表（左侧资源库） |

---

## 5. 前端组件详细设计（简化版）

### 5.1 ScreenDetail.vue（单一组件）

#### 5.1.1 核心状态管理
```javascript
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import draggable from 'vuedraggable'
import { 
  getScreenById, 
  getAvailableFields,
  addTab,
  deleteTab,
  reorderTabs,
  addFieldToScreen,
  removeFieldFromScreen,
  reorderFields
} from '@/api/screen'

const route = useRoute()
const router = useRouter()

// ========== 核心状态 ==========
const screen = ref({})
const availableFields = ref([])
const loading = ref(false)
const saving = ref(false)
const searchKeyword = ref('') // 字段搜索关键词
const systemExpanded = ref(true) // 系统字段展开状态
const customExpanded = ref(false) // 自定义字段展开状态

// ========== 计算属性 ==========
// 已添加的字段 ID 集合（用于禁用已添加字段）
const addedFieldIds = computed(() => {
  const ids = new Set()
  screen.value.tabs?.forEach(tab => {
    tab.items?.forEach(item => {
      ids.add(item.fieldDefinitionId)
    })
  })
  return ids
})

// 过滤后的系统字段
const filteredSystemFields = computed(() => {
  return availableFields.value.filter(f => 
    f.isSystem && 
    f.fieldName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 过滤后的自定义字段
const filteredCustomFields = computed(() => {
  return availableFields.value.filter(f => 
    !f.isSystem && 
    f.fieldName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// ========== 数据加载 ==========
const loadData = async () => {
  const id = route.params.id
  if (!id) return
  
  loading.value = true
  try {
    const [screenRes, fieldsRes] = await Promise.all([
      getScreenById(id),
      getAvailableFields()
    ])
    screen.value = screenRes
    availableFields.value = fieldsRes
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// ========== Tab 操作 ==========
const handleAddTab = async () => {
  try {
    const { value: tabName } = await ElMessageBox.prompt(
      '请输入 Tab 名称',
      '新建 Tab',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: 'Tab 名称不能为空'
      }
    )
    
    await addTab(screen.value.id, tabName)
    ElMessage.success('添加成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('添加失败：' + error.message)
    }
  }
}

const handleDeleteTab = async (tab) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 Tab“${tab.tabName}”吗？该 Tab 下的所有字段也将被删除。`,
      '警告',
      { type: 'warning' }
    )
    
    await deleteTab(tab.id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

const handleReorderTabs = async () => {
  if (screen.value.isSystem) return
  
  try {
    const tabIds = screen.value.tabs.map(tab => tab.id)
    await reorderTabs(screen.value.id, tabIds)
    ElMessage.success('Tab 顺序已更新')
  } catch (error) {
    ElMessage.error('更新失败：' + error.message)
    await loadData() // 恢复原顺序
  }
}

// ========== 字段操作 ==========
const handleDropField = async ({ tab, field }) => {
  // 校验：字段是否已添加
  if (addedFieldIds.value.has(field.id)) {
    ElMessage.warning(`字段“${field.fieldName}”已在当前 Screen 中`)
    return
  }
  
  try {
    await addFieldToScreen(screen.value.id, {
      fieldDefinitionId: field.id,
      screenTabId: tab.id
    })
    ElMessage.success('字段添加成功')
    await loadData()
  } catch (error) {
    ElMessage.error('添加失败：' + error.message)
  }
}

const handleRemoveField = async (item, tab) => {
  try {
    await ElMessageBox.confirm(
      `确定要从 Tab“${tab.tabName}”中移除字段“${item.fieldName}”吗？`,
      '警告',
      { type: 'warning' }
    )
    
    await removeFieldFromScreen(item.id)
    ElMessage.success('移除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('移除失败：' + error.message)
    }
  }
}

const handleReorderFields = async (tab) => {
  if (screen.value.isSystem) return
  
  try {
    const itemIds = tab.items.map(item => item.id)
    await reorderFields(screen.value.id, itemIds)
    ElMessage.success('字段顺序已更新')
  } catch (error) {
    ElMessage.error('更新失败：' + error.message)
    await loadData() // 恢复原顺序
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  loadData()
})
</script>
```

// 拖拽开始
const handleFieldDragStart = (field) => {
  // 存储拖拽数据
  sessionStorage.setItem('draggingField', JSON.stringify(field))
}

// 放置字段
const handleDropField = async ({ tabId, position }) => {
  const fieldData = JSON.parse(sessionStorage.getItem('draggingField'))
  if (!fieldData) return
  
  try {
    await addFieldToScreen(screen.value.id, {
      fieldDefinitionId: fieldData.id,
      screenTabId: tabId,
      insertPosition: position // 'before' | 'after' | 'end'
    })
    ElMessage.success('字段添加成功')
    await loadData() // 重新加载以获取最新顺序
  } catch (error) {
    ElMessage.error('添加失败：' + error.message)
  }
}

// 保存
const handleSave = async () => {
  saving.value = true
  try {
    // 所有更改已实时保存，此处仅提示
    ElMessage.success('保存成功')
    isDirty.value = false
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

// 取消
const handleCancel = () => {
  if (isDirty.value) {
    ElMessageBox.confirm('有未保存的更改，确定要离开吗？', '警告')
      .then(() => router.back())
      .catch(() => {})
  } else {
    router.back()
  }
}

onMounted(() => {
  loadData()
})
</script>
```

#### 5.1.3 样式
```scss
.screen-config-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  
  .config-body {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    // 左侧固定宽度，右侧自适应
    > *:first-child {
      width: 300px;
      flex-shrink: 0;
    }
    
    > *:last-child {
      flex: 1;
      overflow-y: auto;
    }
  }
}
```

---

### 5.2 FieldLibrary.vue（字段资源库）

#### 5.2.1 模板
```vue
<template>
  <div class="field-library">
    <!-- 搜索框 -->
    <el-input
      v-model="searchKeyword"
      placeholder="搜索字段..."
      prefix-icon="Search"
      clearable
    />

    <!-- 系统字段 -->
    <div class="field-group">
      <div class="group-header" @click="toggleSystemFields">
        <el-icon><ArrowRight v-if="!systemExpanded" /><ArrowDown v-else /></el-icon>
        <span>系统字段</span>
      </div>
      
      <draggable
        v-show="systemExpanded"
        :list="systemFields"
        :group="{ name: 'fields', pull: 'clone', put: false }"
        :sort="false"
        item-key="id"
        @start="handleDragStart"
      >
        <template #item="{ element }">
          <FieldItem 
            :field="element" 
            :disabled="isFieldAdded(element.id)"
          />
        </template>
      </draggable>
    </div>

    <!-- 自定义字段 -->
    <div class="field-group">
      <div class="group-header" @click="toggleCustomFields">
        <el-icon><ArrowRight v-if="!customExpanded" /><ArrowDown v-else /></el-icon>
        <span>自定义字段 ({{ customFields.length }})</span>
      </div>
      
      <draggable
        v-show="customExpanded"
        :list="customFields"
        :group="{ name: 'fields', pull: 'clone', put: false }"
        :sort="false"
        item-key="id"
        @start="handleDragStart"
      >
        <template #item="{ element }">
          <FieldItem 
            :field="element" 
            :disabled="isFieldAdded(element.id)"
          />
        </template>
      </draggable>
    </div>
  </div>
</template>
```

#### 5.2.2 脚本
```javascript
<script setup>
import { ref, computed } from 'vue'
import draggable from 'vuedraggable'
import FieldItem from './FieldItem.vue'

const props = defineProps({
  fields: Array,
  addedFieldIds: Set
})

const emit = defineEmits(['drag-start'])

const searchKeyword = ref('')
const systemExpanded = ref(true)
const customExpanded = ref(false)

// 过滤后的字段列表
const systemFields = computed(() => {
  return props.fields.filter(f => 
    f.isSystem && 
    f.fieldName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const customFields = computed(() => {
  return props.fields.filter(f => 
    !f.isSystem && 
    f.fieldName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

const toggleSystemFields = () => {
  systemExpanded.value = !systemExpanded.value
}

const toggleCustomFields = () => {
  customExpanded.value = !customExpanded.value
}

const isFieldAdded = (fieldId) => {
  return props.addedFieldIds.has(fieldId)
}

const handleDragStart = (evt) => {
  emit('drag-start', evt.item.__draggable_context.element)
}
</script>
```

---

### 5.3 CanvasArea.vue（画布区）

#### 5.3.1 模板
```vue
<template>
  <div class="canvas-area">
    <draggable
      :list="tabs"
      item-key="id"
      handle=".tab-drag-handle"
      :disabled="isSystem"
      @end="handleTabReorder"
      class="tabs-container"
    >
      <template #item="{ element: tab }">
        <TabContainer
          :tab="tab"
          :is-system="isSystem"
          @delete="handleDeleteTab"
          @drop-field="handleDropField"
          @remove-field="handleRemoveField"
          @reorder-fields="handleReorderFields"
        />
      </template>
    </draggable>

    <!-- 新建 Tab 按钮 -->
    <el-button
      v-if="!isSystem"
      @click="handleAddTab"
      class="add-tab-btn"
    >
      <el-icon><Plus /></el-icon>
      新建 Tab
    </el-button>
  </div>
</template>
```

#### 5.3.2 脚本
```javascript
<script setup>
import draggable from 'vuedraggable'
import TabContainer from './TabContainer.vue'

const props = defineProps({
  tabs: Array,
  isSystem: Boolean
})

const emit = defineEmits([
  'add-tab',
  'delete-tab',
  'reorder-tabs',
  'drop-field',
  'remove-field',
  'reorder-fields'
])

const handleTabReorder = () => {
  const tabIds = props.tabs.map(tab => tab.id)
  emit('reorder-tabs', tabIds)
}

const handleAddTab = () => {
  emit('add-tab')
}

const handleDeleteTab = (tabId) => {
  emit('delete-tab', tabId)
}

const handleDropField = (data) => {
  emit('drop-field', data)
}

const handleRemoveField = (itemId) => {
  emit('remove-field', itemId)
}

const handleReorderFields = (data) => {
  emit('reorder-fields', data)
}
</script>
```

---

### 5.4 TabContainer.vue（Tab 容器）

#### 5.4.1 模板
```vue
<template>
  <el-card class="tab-container" shadow="hover">
    <!-- Tab 头部 -->
    <template #header>
      <div class="tab-header">
        <el-icon class="tab-drag-handle"><Rank /></el-icon>
        
        <el-input
          v-if="editing"
          v-model="tabName"
          size="small"
          @blur="saveTabName"
          @keyup.enter="saveTabName"
          autofocus
        />
        <span v-else @dblclick="startEdit">{{ tab.tabName }}</span>
        
        <el-button
          v-if="!isSystem"
          link
          type="danger"
          size="small"
          @click="handleDelete"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </template>

    <!-- 字段列表 -->
    <draggable
      :list="tab.items"
      item-key="id"
      group="fields"
      handle=".field-drag-handle"
      :disabled="isSystem"
      @end="handleFieldReorder"
      class="field-list"
      :class="{ 'empty': !tab.items || tab.items.length === 0 }"
    >
      <template #item="{ element: item }">
        <div class="field-item">
          <el-icon class="field-drag-handle"><Rank /></el-icon>
          
          <div class="field-info">
            <div class="field-name">{{ item.fieldName }}</div>
            <div class="field-meta">
              <span class="field-key">{{ item.fieldKey }}</span>
              <el-tag size="small">{{ item.fieldType }}</el-tag>
            </div>
          </div>
          
          <el-button
            v-if="!isSystem"
            link
            type="danger"
            size="small"
            @click="handleRemove(item.id)"
          >
            移除
          </el-button>
        </div>
      </template>
      
      <!-- 空状态 -->
      <template #footer>
        <div v-if="!tab.items || tab.items.length === 0" class="empty-state">
          <el-icon><Document /></el-icon>
          <p>拖拽左侧字段到此处</p>
        </div>
      </template>
    </draggable>
  </el-card>
</template>
```

---

## 6. 拖拽实现细节（优化版）

### 6.1 vuedraggable 配置

#### 左侧资源库（克隆模式）
```vue
<draggable
  :list="filteredSystemFields"
  :group="{ name: 'fields', pull: 'clone', put: false }"
  :sort="false"
  item-key="id"
  class="field-list"
>
  <template #item="{ element }">
    <div 
      class="field-item"
      :class="{ 'disabled': addedFieldIds.has(element.id) }"
    >
      <el-icon><Rank /></el-icon>
      <div class="field-info">
        <div class="field-name">{{ element.fieldName }}</div>
        <div class="field-meta">
          <span>{{ element.fieldKey }}</span>
          <el-tag size="small">{{ element.fieldType }}</el-tag>
        </div>
      </div>
    </div>
  </template>
</draggable>
```

**关键配置**：
- `pull: 'clone'`：拖拽时克隆元素，不从原列表移除
- `put: false`：不允许从其他地方拖入
- `sort: false`：禁止在资源库内排序

#### 右侧 Tab 字段列表（移动模式）
```vue
<draggable
  :list="tab.items"
  group="fields"
  item-key="id"
  handle=".drag-handle"
  :disabled="screen.isSystem"
  @end="() => handleReorderFields(tab)"
  class="field-list"
>
  <template #item="{ element }">
    <div class="field-item">
      <el-icon class="drag-handle"><Rank /></el-icon>
      <div class="field-info">
        <div class="field-name">{{ element.fieldName }}</div>
        <div class="field-meta">
          <span>{{ element.fieldKey }}</span>
          <el-tag size="small">{{ element.fieldType }}</el-tag>
        </div>
      </div>
      <el-button 
        link 
        type="danger" 
        size="small"
        @click="handleRemoveField(element, tab)"
      >
        移除
      </el-button>
    </div>
  </template>
</draggable>
```

**关键配置**：
- `group: 'fields'`：与左侧同名，允许接收拖拽
- `handle: '.drag-handle'`：只有拖拽手柄可触发
- `@end`：拖拽结束时触发排序保存

### 6.2 跨容器拖拽处理

vuedraggable 自动处理跨容器拖拽，无需手动监听：

```javascript
// 当从左侧拖拽到右侧时，vuedraggable 会自动：
// 1. 触发 add 事件
// 2. 将字段添加到目标列表
// 3. 我们需要监听这个变化并调用 API

// 使用 watch 监听 tab.items 变化
watch(
  () => screen.value.tabs,
  (newTabs, oldTabs) => {
    // 检测是否有新增字段
    newTabs.forEach((tab, index) => {
      const oldItems = oldTabs[index]?.items || []
      const newItems = tab.items || []
      
      if (newItems.length > oldItems.length) {
        // 有新增字段，找到新增的字段
        const addedItem = newItems.find(
          newItem => !oldItems.some(oldItem => oldItem.id === newItem.id)
        )
        
        if (addedItem) {
          // 调用 API 添加字段
          handleDropField({ tab, field: addedItem })
        }
      }
    })
  },
  { deep: true }
)
```

**优化说明**：
- 不再使用 sessionStorage 传递数据
- 直接通过响应式数据变化检测
- 更可靠，无浏览器兼容问题

---

## 7. 样式设计

### 7.1 主题色
```scss
$primary-color: #409EFF;      // 主色调（Element Plus Blue）
$success-color: #67C23A;      // 成功
$warning-color: #E6A23C;      // 警告
$danger-color: #F56C6C;       // 危险
$info-color: #909399;         // 信息

$border-color: #DCDFE6;       // 边框
$bg-color: #F5F7FA;           // 背景
$text-primary: #303133;       // 主文字
$text-regular: #606266;       // 常规文字
$text-secondary: #909399;     // 次要文字
```

### 7.2 拖拽样式
```scss
// 拖拽手柄
.drag-handle {
  cursor: move;
  color: $text-secondary;
  margin-right: 8px;
  
  &:hover {
    color: $primary-color;
  }
}

// 拖拽中的元素
.sortable-drag {
  opacity: 0.8;
  background: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: rotate(2deg);
}

// 占位符
.sortable-ghost {
  opacity: 0.4;
  background: #c8ebfb;
  border: 2px dashed $primary-color;
}

// 选中状态
.sortable-chosen {
  background: #ecf5ff;
  border-color: $primary-color;
}
```

### 7.3 字段项样式
```scss
.field-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  margin-bottom: 8px;
  background: #fff;
  border: 1px solid $border-color;
  border-radius: 4px;
  transition: all 0.3s;
  
  &:hover {
    border-color: $primary-color;
    box-shadow: 0 2px 8px rgba($primary-color, 0.1);
  }
  
  .field-info {
    flex: 1;
    margin-left: 8px;
    
    .field-name {
      font-weight: 500;
      color: $text-primary;
    }
    
    .field-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 4px;
      
      .field-key {
        font-size: 12px;
        color: $text-secondary;
      }
    }
  }
}
```

---

## 8. 状态管理（简化）

**不使用 Pinia**，原因：
- 当前功能仅在单页面使用
- Vue 3 Composition API 的 `ref/reactive` 足够
- 减少依赖和复杂度

**状态定义**：直接在 ScreenDetail.vue 中使用 `ref` 和 `computed`

---

## 9. 错误处理

### 9.1 API 错误拦截
```javascript
// utils/request.js
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response) {
      const { status, data } = error.response
      
      switch (status) {
        case 400:
          ElMessage.error(data.message || '请求参数错误')
          break
        case 403:
          ElMessage.error('无权限操作')
          break
        case 404:
          ElMessage.error('资源不存在')
          break
        case 500:
          ElMessage.error('服务器错误')
          break
        default:
          ElMessage.error('网络错误')
      }
    } else {
      ElMessage.error('网络连接失败')
    }
    
    return Promise.reject(error)
  }
)
```

### 9.2 并发冲突处理
```javascript
const handleSave = async () => {
  try {
    await saveScreenConfig(screen.value.id, config)
  } catch (error) {
    if (error.response?.status === 409) {
      // 并发冲突
      ElMessageBox.confirm(
        '配置已被他人修改，是否覆盖？',
        '冲突检测',
        {
          confirmButtonText: '覆盖',
          cancelButtonText: '重新加载',
          type: 'warning'
        }
      ).then(async () => {
        // 强制保存
        await forceSave()
      }).catch(() => {
        // 重新加载
        await loadData()
      })
    }
  }
}
```

---

## 10. 性能优化（精简版）

### 10.1 当前数据量评估
- **字段总数**：< 50 个（系统 + 自定义）
- **Tab 数量**：< 10 个
- **每 Tab 字段数**：< 20 个

**结论**：无需虚拟滚动、懒加载等复杂优化

### 10.2 必要的优化

#### 防抖搜索
```javascript
import { ref, watch } from 'vue'
import { debounce } from 'lodash-es'

const searchKeyword = ref('')

// 防抖搜索（300ms）
watch(searchKeyword, debounce(() => {
  // 搜索逻辑已在 computed 中自动执行
}, 300))
```

### 10.3 未来优化方向（数据量增长后）
1. **虚拟滚动**：字段 > 100 时启用
2. **分页加载**：自定义字段过多时分页
3. **缓存策略**：LocalStorage 缓存字段列表

---

## 11. 测试计划

### 11.1 单元测试
```javascript
// tests/unit/ScreenDetail.spec.js
import { mount } from '@vue/test-utils'
import ScreenDetail from '@/views/config/ScreenDetail.vue'

describe('ScreenDetail', () => {
  it('应该正确加载 Screen 数据', async () => {
    const wrapper = mount(ScreenDetail)
    await nextTick()
    
    expect(wrapper.vm.screen).toBeDefined()
    expect(wrapper.vm.screen.tabs).toBeInstanceOf(Array)
  })
  
  it('拖拽字段后应该调用 API', async () => {
    const wrapper = mount(ScreenDetail)
    const mockAddField = jest.fn()
    
    // 模拟拖拽
    await wrapper.vm.handleDropField({ tabId: 1, fieldId: 2 })
    
    expect(mockAddField).toHaveBeenCalledWith(expect.objectContaining({
      fieldDefinitionId: 2,
      screenTabId: 1
    }))
  })
})
```

### 11.2 E2E 测试
```javascript
// tests/e2e/screen-config.cy.js
describe('Screen 配置', () => {
  beforeEach(() => {
    cy.visit('/config/screens/1')
  })
  
  it('应该能拖拽字段到 Tab', () => {
    cy.get('.field-library .field-item').first().as('source')
    cy.get('.tab-container .field-list').first().as('target')
    
    cy.get('@source').trigger('mousedown', { which: 1 })
    cy.get('@target').trigger('mousemove').trigger('mouseup')
    
    cy.contains('字段添加成功').should('be.visible')
  })
  
  it('应该能调整 Tab 顺序', () => {
    cy.get('.tab-container').first().as('firstTab')
    cy.get('.tab-container').eq(1).as('secondTab')
    
    cy.get('@firstTab').find('.tab-drag-handle')
      .trigger('mousedown', { which: 1 })
    cy.get('@secondTab').trigger('mousemove').trigger('mouseup')
    
    cy.contains('Tab顺序已更新').should('be.visible')
  })
})
```

---

## 12. 部署清单

### 12.1 前端
- [ ] 安装依赖：`npm install vuedraggable@next`
- [ ] 构建生产版本：`npm run build`
- [ ] 检查打包体积：< 500KB（gzip）

### 12.2 后端
- [ ] 数据库迁移脚本已执行
- [ ] API 端点已测试
- [ ] 权限控制已配置

### 12.3 监控
- [ ] 添加性能监控（首屏加载时间）
- [ ] 添加错误追踪（Sentry）
- [ ] 添加用户行为分析（可选）

---

## 13. 后续优化方向

### 13.1 P1（下个迭代）
1. **批量操作**：支持多选字段批量添加/移除
2. **撤销重做**：实现操作历史栈
3. **模板功能**：保存常用配置为模板

### 13.2 P2（未来规划）
4. **条件显示**：配置字段显示/隐藏规则
5. **字段联动**：根据某字段值动态显示其他字段
6. **国际化**：支持多语言界面

---

**文档版本**: V2.0  
**最后更新**: 2026-04-13  
**作者**: Lingma AI Assistant
