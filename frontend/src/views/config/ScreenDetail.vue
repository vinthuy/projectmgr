<template>
  <div class="screen-config-container">
    <!-- 顶部操作栏 -->
    <div class="config-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="page-title">{{ screen.screenName || '屏幕配置' }}</span>
          <el-tag v-if="screen.isSystem" type="success" size="small" style="margin-left: 10px">系统</el-tag>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="config-body" v-loading="loading">
      <!-- 左侧字段资源库 -->
      <div class="field-library">
        <div class="library-header">
          <h3>字段资源库</h3>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索字段..."
            prefix-icon="Search"
            clearable
            size="small"
          />
        </div>

        <div class="library-content">
          <!-- 系统字段 -->
          <div class="field-group">
            <div class="group-header" @click="systemExpanded = !systemExpanded">
              <el-icon><ArrowRight v-if="!systemExpanded" /><ArrowDown v-else /></el-icon>
              <span>系统字段</span>
            </div>
            
            <draggable
              v-show="systemExpanded"
              :list="filteredSystemFields"
              :group="{ name: 'fields', pull: 'clone', put: false }"
              :sort="false"
              item-key="id"
              class="field-list"
              @start="handleDragStart"
            >
              <template #item="{ element }">
                <div 
                  class="field-item"
                  :class="{ 'disabled': addedFieldIds.has(element.id) }"
                >
                  <el-icon class="drag-icon"><Rank /></el-icon>
                  <div class="field-info">
                    <div class="field-name">{{ element.fieldName }}</div>
                    <div class="field-meta">
                      <span class="field-key">{{ element.fieldKey }}</span>
                      <el-tag size="small" type="info">{{ element.fieldType }}</el-tag>
                    </div>
                  </div>
                </div>
              </template>
            </draggable>
          </div>

          <!-- 自定义字段 -->
          <div class="field-group">
            <div class="group-header" @click="customExpanded = !customExpanded">
              <el-icon><ArrowRight v-if="!customExpanded" /><ArrowDown v-else /></el-icon>
              <span>自定义字段 ({{ filteredCustomFields.length }})</span>
            </div>
            
            <draggable
              v-show="customExpanded"
              :list="filteredCustomFields"
              :group="{ name: 'fields', pull: 'clone', put: false }"
              :sort="false"
              item-key="id"
              class="field-list"
              @start="handleDragStart"
            >
              <template #item="{ element }">
                <div 
                  class="field-item"
                  :class="{ 'disabled': addedFieldIds.has(element.id) }"
                >
                  <el-icon class="drag-icon"><Rank /></el-icon>
                  <div class="field-info">
                    <div class="field-name">{{ element.fieldName }}</div>
                    <div class="field-meta">
                      <span class="field-key">{{ element.fieldKey }}</span>
                      <el-tag size="small" type="info">{{ element.fieldType }}</el-tag>
                    </div>
                  </div>
                </div>
              </template>
            </draggable>
          </div>
        </div>
      </div>

      <!-- 右侧画布区 -->
      <div class="canvas-area">
        <draggable
          v-model="screen.tabs"
          item-key="id"
          handle=".tab-drag-handle"
          :disabled="screen.isSystem"
          @end="handleReorderTabs"
          class="tabs-container"
        >
          <template #item="{ element: tab }">
            <el-card class="tab-card" shadow="hover">
              <template #header>
                <div class="tab-header">
                  <el-icon class="tab-drag-handle"><Rank /></el-icon>
                  <span class="tab-name">{{ tab.tabName }}</span>
                  <el-button
                    v-if="!screen.isSystem"
                    link
                    type="danger"
                    size="small"
                    @click="handleDeleteTab(tab)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </template>

              <!-- 字段列表 -->
              <draggable
                :list="tab.items"
                group="fields"
                item-key="id"
                handle=".field-drag-handle"
                :disabled="screen.isSystem"
                @end="() => handleReorderFields(tab)"
                class="tab-field-list"
                :class="{ 'empty': !tab.items || tab.items.length === 0 }"
                @add="(evt) => handleDropField(evt, tab)"
              >
                <template #item="{ element: item }">
                  <div class="tab-field-item">
                    <el-icon class="field-drag-handle"><Rank /></el-icon>
                    <div class="field-info">
                      <div class="field-name">{{ item.fieldName }}</div>
                      <div class="field-meta">
                        <span class="field-key">{{ item.fieldKey }}</span>
                        <el-tag size="small">{{ item.fieldType }}</el-tag>
                      </div>
                    </div>
                    <el-button
                      v-if="!screen.isSystem"
                      link
                      type="danger"
                      size="small"
                      @click="handleRemoveField(item, tab)"
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
        </draggable>

        <!-- 新建 Tab 按钮 -->
        <el-button
          v-if="!screen.isSystem"
          @click="handleAddTab"
          class="add-tab-btn"
          :icon="Plus"
        >
          新建 Tab
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Rank, Delete, Document, ArrowRight, ArrowDown, Search } from '@element-plus/icons-vue'
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
const searchKeyword = ref('')
const systemExpanded = ref(true)
const customExpanded = ref(false)
const draggingField = ref(null)

// ========== 计算属性 ==========
const addedFieldIds = computed(() => {
  const ids = new Set()
  screen.value.tabs?.forEach(tab => {
    tab.items?.forEach(item => {
      ids.add(item.fieldDefinitionId)
    })
  })
  return ids
})

const filteredSystemFields = computed(() => {
  return availableFields.value.filter(f => 
    f.isSystem && 
    f.fieldName.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

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
    
    // 调试日志
    console.log('Screen loaded:', screen.value.screenName)
    console.log('Tabs count:', screen.value.tabs?.length)
    if (screen.value.tabs && screen.value.tabs.length > 0) {
      console.log('First tab:', screen.value.tabs[0].tabName)
      console.log('First tab items:', screen.value.tabs[0].items)
      console.log('First tab items type:', typeof screen.value.tabs[0].items)
      console.log('First tab items length:', screen.value.tabs[0].items?.length)
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
    console.error('Load data error:', error)
  } finally {
    loading.value = false
  }
}

// ========== 拖拽处理 ==========
const handleDragStart = (evt) => {
  draggingField.value = evt.item.__draggable_context.element
}

const handleDropField = async (evt, tab) => {
  // 从 evt.added 获取新添加的字段
  const addedItem = evt.added?.element
  if (!addedItem) return
  
  // 校验：字段是否已添加
  if (addedFieldIds.value.has(addedItem.fieldDefinitionId)) {
    ElMessage.warning(`字段"${addedItem.fieldName}"已在当前 Screen 中`)
    // 移除刚添加的项（通过 fieldDefinitionId 和 screenTabId 匹配）
    const index = tab.items.findIndex(item => 
      item.fieldDefinitionId === addedItem.fieldDefinitionId && 
      item.screenTabId === tab.id
    )
    if (index > -1) {
      tab.items.splice(index, 1)
    }
    return
  }
  
  try {
    await addFieldToScreen(screen.value.id, {
      fieldDefinitionId: addedItem.fieldDefinitionId,
      screenTabId: tab.id
    })
    ElMessage.success('字段添加成功')
    // 重新加载以获取后端返回的真实数据
    await loadData()
  } catch (error) {
    ElMessage.error('添加失败：' + error.message)
    // 移除失败的项（通过 fieldDefinitionId 和 screenTabId 匹配）
    const index = tab.items.findIndex(item => 
      item.fieldDefinitionId === addedItem.fieldDefinitionId && 
      item.screenTabId === tab.id
    )
    if (index > -1) {
      tab.items.splice(index, 1)
    }
    await loadData() // 恢复原状态
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
      `确定要删除 Tab"${tab.tabName}"吗？该 Tab 下的所有字段也将被删除。`,
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
    await loadData()
  }
}

// ========== 字段操作 ==========
const handleRemoveField = async (item, tab) => {
  try {
    await ElMessageBox.confirm(
      `确定要从 Tab"${tab.tabName}"中移除字段"${item.fieldName}"吗？`,
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
    await loadData()
  }
}

// ========== 其他操作 ==========
const handleSave = async () => {
  saving.value = true
  try {
    // 所有更改已实时保存
    ElMessage.success('保存成功')
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
  } finally {
    saving.value = false
  }
}

const goBack = () => {
  router.push('/config/screens')
}

// ========== 生命周期 ==========
onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.screen-config-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  
  .config-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #dcdfe6;
    
    .page-title {
      font-size: 18px;
      font-weight: 600;
    }
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .config-body {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    // 左侧固定宽度
    .field-library {
      width: 320px;
      flex-shrink: 0;
      background: #fff;
      border-right: 1px solid #dcdfe6;
      display: flex;
      flex-direction: column;
      
      .library-header {
        padding: 16px;
        border-bottom: 1px solid #dcdfe6;
        
        h3 {
          margin: 0 0 12px 0;
          font-size: 16px;
          font-weight: 600;
        }
      }
      
      .library-content {
        flex: 1;
        overflow-y: auto;
        padding: 16px;
        
        .field-group {
          margin-bottom: 16px;
          
          .group-header {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 8px 0;
            cursor: pointer;
            font-weight: 500;
            color: #303133;
            
            &:hover {
              color: #409eff;
            }
          }
          
          .field-list {
            margin-top: 8px;
            
            .field-item {
              display: flex;
              align-items: center;
              padding: 10px 12px;
              margin-bottom: 8px;
              background: #fff;
              border: 1px solid #dcdfe6;
              border-radius: 4px;
              cursor: move;
              transition: all 0.3s;
              
              &:hover {
                border-color: #409eff;
                box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
              }
              
              &.disabled {
                opacity: 0.5;
                cursor: not-allowed;
                
                &:hover {
                  border-color: #dcdfe6;
                  box-shadow: none;
                }
              }
              
              .drag-icon {
                color: #909399;
                margin-right: 8px;
                cursor: move;
                
                &:hover {
                  color: #409eff;
                }
              }
              
              .field-info {
                flex: 1;
                
                .field-name {
                  font-size: 14px;
                  font-weight: 500;
                  color: #303133;
                  margin-bottom: 4px;
                }
                
                .field-meta {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  
                  .field-key {
                    font-size: 12px;
                    color: #909399;
                  }
                }
              }
            }
          }
        }
      }
    }
    
    // 右侧自适应
    .canvas-area {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
      
      .tabs-container {
        display: flex;
        flex-direction: column;
        gap: 16px;
        
        .tab-card {
          .tab-header {
            display: flex;
            align-items: center;
            gap: 8px;
            
            .tab-drag-handle {
              cursor: move;
              color: #909399;
              
              &:hover {
                color: #409eff;
              }
            }
            
            .tab-name {
              flex: 1;
              font-size: 16px;
              font-weight: 600;
            }
          }
          
          .tab-field-list {
            min-height: 60px;
            
            &.empty {
              display: flex;
              align-items: center;
              justify-content: center;
              min-height: 100px;
              border: 2px dashed #dcdfe6;
              border-radius: 4px;
              
              .empty-state {
                text-align: center;
                color: #909399;
                
                .el-icon {
                  font-size: 32px;
                  margin-bottom: 8px;
                }
                
                p {
                  margin: 0;
                  font-size: 14px;
                }
              }
            }
            
            .tab-field-item {
              display: flex;
              align-items: center;
              padding: 12px 16px;
              margin-bottom: 8px;
              background: #fff;
              border: 1px solid #dcdfe6;
              border-radius: 4px;
              transition: all 0.3s;
              
              &:hover {
                border-color: #409eff;
                box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
              }
              
              .field-drag-handle {
                cursor: move;
                color: #909399;
                margin-right: 10px;
                
                &:hover {
                  color: #409eff;
                }
              }
              
              .field-info {
                flex: 1;
                
                .field-name {
                  font-size: 14px;
                  font-weight: 500;
                  color: #303133;
                  margin-bottom: 4px;
                }
                
                .field-meta {
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  
                  .field-key {
                    font-size: 12px;
                    color: #909399;
                  }
                }
              }
            }
          }
        }
        
        // 拖拽样式
        :deep(.sortable-ghost) {
          opacity: 0.4;
          background: #c8ebfb;
          border: 2px dashed #409eff;
        }
        
        :deep(.sortable-drag) {
          opacity: 0.8;
          background: #fff;
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
          transform: rotate(2deg);
        }
      }
      
      .add-tab-btn {
        margin-top: 16px;
        width: 100%;
      }
    }
  }
}
</style>
