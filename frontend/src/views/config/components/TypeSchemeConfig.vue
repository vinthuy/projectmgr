<template>
  <div class="type-scheme-config">
    <el-form :model="form" label-width="100px">
      <el-form-item label="方案Key" required>
        <el-input v-model="form.schemeKey" :disabled="!!scheme.id" placeholder="例如: software-types" />
      </el-form-item>
      <el-form-item label="方案名称" required>
        <el-input v-model="form.schemeName" placeholder="例如: 软件开发类型方案" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" placeholder="方案描述..." />
      </el-form-item>
    </el-form>

    <el-divider content-position="left">包含的工作项类型</el-divider>

    <div class="type-selector">
      <div class="available-types">
        <h4>可用类型</h4>
        <el-checkbox-group v-model="selectedTypeIds">
          <div v-for="type in availableTypes" :key="type.id" class="type-item">
            <el-checkbox :label="type.id">
              <el-icon><component :is="getIconComponent(type.icon)" /></el-icon>
              {{ type.typeName }}
            </el-checkbox>
          </div>
        </el-checkbox-group>
      </div>

      <div class="selected-types">
        <h4>已选类型 (拖拽排序)</h4>
        <draggable 
          v-model="orderedSelectedTypes" 
          item-key="id"
          class="type-list"
          @end="handleDragEnd"
        >
          <template #item="{ element }">
            <div class="type-card">
              <el-icon class="drag-handle"><Rank /></el-icon>
              <el-icon><component :is="getIconComponent(element.icon)" /></el-icon>
              <span>{{ element.typeName }}</span>
              <el-button link type="danger" size="small" @click="removeType(element.id)">
                <el-icon><Close /></el-icon>
              </el-button>
            </div>
          </template>
        </draggable>
      </div>
    </div>

    <el-divider />

    <el-form-item>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </el-form-item>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Rank, Close, Star, Bookmark, Document, BugReport, Tickets } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { getTypeSchemeDetail, createTypeScheme, updateTypeScheme, batchUpdateTypeSchemeMappings } from '@/api/typeScheme'
import { getWorkItemTypes } from '@/api/workItemType'

const props = defineProps({ 
  scheme: Object 
})
const emit = defineEmits(['save', 'cancel'])

const form = ref({
  schemeKey: '',
  schemeName: '',
  description: ''
})

const allTypes = ref([])
const selectedTypeIds = ref([])
const orderedSelectedTypes = ref([])
const saving = ref(false)

// 可用类型(未选择的)
const availableTypes = computed(() => {
  return allTypes.value.filter(t => !selectedTypeIds.value.includes(t.id))
})

// 获取图标组件
const getIconComponent = (iconName) => {
  const icons = { Star, Bookmark, Document, BugReport, Tickets }
  return icons[iconName] || Document
}

// 拖拽结束
const handleDragEnd = () => {
  // 更新顺序
}

// 移除类型
const removeType = (typeId) => {
  selectedTypeIds.value = selectedTypeIds.value.filter(id => id !== typeId)
  orderedSelectedTypes.value = orderedSelectedTypes.value.filter(t => t.id !== typeId)
}

// 监听选中变化
watch(selectedTypeIds, (newIds) => {
  // 同步到orderedSelectedTypes
  const newOrdered = []
  newIds.forEach(id => {
    const existing = orderedSelectedTypes.value.find(t => t.id === id)
    if (existing) {
      newOrdered.push(existing)
    } else {
      const type = allTypes.value.find(t => t.id === id)
      if (type) newOrdered.push(type)
    }
  })
  orderedSelectedTypes.value = newOrdered
})

// 加载数据
const loadData = async () => {
  try {
    const [typesRes, detailRes] = await Promise.all([
      getWorkItemTypes(),
      props.scheme.id ? getTypeSchemeDetail(props.scheme.id) : Promise.resolve(null)
    ])
    
    allTypes.value = typesRes || []
    
    if (props.scheme.id && detailRes) {
      // 编辑模式:加载已有映射
      form.value = {
        schemeKey: props.scheme.schemeKey,
        schemeName: props.scheme.schemeName,
        description: props.scheme.description
      }
      
      selectedTypeIds.value = detailRes.map(m => m.issueTypeId)
      orderedSelectedTypes.value = detailRes.map(m => {
        const type = allTypes.value.find(t => t.id === m.issueTypeId)
        return type ? { ...type, displayOrder: m.displayOrder } : null
      }).filter(Boolean)
    } else {
      // 新建模式
      form.value = {
        schemeKey: props.scheme.schemeKey || '',
        schemeName: props.scheme.schemeName || '',
        description: props.scheme.description || ''
      }
    }
  } catch (error) {
    ElMessage.error('加载数据失败: ' + error.message)
  }
}

// 保存
const handleSave = async () => {
  if (!form.value.schemeKey || !form.value.schemeName) {
    ElMessage.warning('请填写方案Key和名称')
    return
  }

  saving.value = true
  try {
    let schemeId = props.scheme.id
    
    // 保存基础信息
    if (schemeId) {
      await updateTypeScheme(schemeId, form.value)
    } else {
      const created = await createTypeScheme(form.value)
      schemeId = created.id
    }
    
    // 保存映射关系
    const mappings = orderedSelectedTypes.value.map((type, index) => ({
      issueTypeId: type.id,
      displayOrder: index
    }))
    
    await batchUpdateTypeSchemeMappings(schemeId, mappings)
    
    ElMessage.success('保存成功')
    emit('save')
  } catch (error) {
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadData()
})

const handleCancel = () => {
  emit('cancel')
}
</script>

<style scoped lang="scss">
.type-scheme-config {
  .type-selector {
    display: flex;
    gap: 20px;
    margin: 20px 0;

    .available-types, .selected-types {
      flex: 1;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      padding: 15px;

      h4 {
        margin: 0 0 15px 0;
        font-size: 14px;
        color: #606266;
      }
    }

    .type-item {
      margin-bottom: 8px;
    }

    .type-list {
      min-height: 200px;
    }

    .type-card {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px;
      margin-bottom: 8px;
      background: #f5f7fa;
      border-radius: 4px;
      cursor: move;

      .drag-handle {
        cursor: grab;
        color: #909399;
      }

      &:active .drag-handle {
        cursor: grabbing;
      }
    }
  }
}
</style>
