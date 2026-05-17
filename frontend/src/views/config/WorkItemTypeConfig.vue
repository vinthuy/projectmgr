<template>
  <div class="config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>工作项类型配置</span>
            <el-select
              v-model="selectedTenantId"
              placeholder="选择租户"
              style="width: 200px; margin-left: 20px"
              @change="handleTenantChange"
            >
              <el-option
                v-for="tenant in tenantList"
                :key="tenant.id"
                :label="tenant.tenantName"
                :value="tenant.id"
              />
            </el-select>
          </div>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增类型
          </el-button>
        </div>
      </template>

      <el-table :data="typeList" border stripe>
        <el-table-column prop="typeKey" label="类型标识" width="150" />
        <el-table-column prop="typeName" label="类型名称" width="150" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="120" align="center">
          <template #default="{ row }">
            <div style="display: flex; align-items: center; justify-content: center; gap: 8px;">
              <el-icon v-if="row.icon" :size="20" :color="getIconColor(row.typeKey)">
                <component :is="row.icon" />
              </el-icon>
              <span style="font-size: 12px; color: #909399;">{{ getIconLabel(row.typeKey) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="typeCategory" label="分类" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.typeCategory === 'SUBTASK' ? 'warning' : 'primary'">
              {{ row.typeCategory === 'SUBTASK' ? '子任务' : '标准' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hierarchyLevel" label="层级" width="80" align="center" />
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="类型标识" prop="typeKey">
          <el-input
            v-model="formData.typeKey"
            placeholder="例如: task, bug, story"
            :disabled="isEdit"
          />
          <div class="form-tip">只能包含小写字母和下划线</div>
        </el-form-item>
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="formData.typeName" placeholder="例如: 任务, 缺陷" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="类型描述"
          />
        </el-form-item>
        <el-form-item label="图标" prop="icon">
          <div class="icon-selector">
            <div 
              v-for="iconOption in iconOptions" 
              :key="iconOption.value"
              class="icon-option"
              :class="{ selected: formData.icon === iconOption.value }"
              @click="formData.icon = iconOption.value"
            >
              <el-icon :size="24" :color="formData.icon === iconOption.value ? '#409EFF' : '#909399'">
                <component :is="iconOption.icon" />
              </el-icon>
              <span class="icon-label">{{ iconOption.label }}</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="分类" prop="typeCategory">
          <el-radio-group v-model="formData.typeCategory">
            <el-radio value="STANDARD">标准类型</el-radio>
            <el-radio value="SUBTASK">子任务</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="层级" prop="hierarchyLevel">
          <el-input-number
            v-model="formData.hierarchyLevel"
            :min="0"
            :max="3"
            placeholder="0=顶层, 1=中层, 2=底层"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getWorkItemTypes, createWorkItemType, updateWorkItemType, deleteWorkItemType } from '@/api/workItemType'
import { getTenants } from '@/api/tenant'

const tenantList = ref([])
const selectedTenantId = ref(1)
const typeList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增工作项类型')
const isEdit = ref(false)
const formRef = ref(null)

const formData = ref({
  typeKey: '',
  typeName: '',
  description: '',
  icon: 'Document',
  typeCategory: 'STANDARD',
  hierarchyLevel: 0
})

// Jira风格的图标选项（参考Jira issue_type设计）
const iconOptions = [
  { value: 'Star', label: '史诗', icon: 'Star' },           // epic - 星星
  { value: 'Bookmark', label: '故事', icon: 'Bookmark' },   // story - 书签
  { value: 'Document', label: '任务', icon: 'Document' },   // task - 文档
  { value: 'BugReport', label: '问题', icon: 'BugReport' }, // bug - 虫子
  { value: 'Tickets', label: '子任务', icon: 'Tickets' },   // subtask - 票据
  { value: 'Flag', label: '标志', icon: 'Flag' },
  { value: 'Connection', label: '链接', icon: 'Connection' },
  { value: 'Collection', label: '集合', icon: 'Collection' },
  { value: 'SetUp', label: '配置', icon: 'SetUp' },
  { value: 'Tools', label: '工具', icon: 'Tools' },
  { value: 'Bell', label: '通知', icon: 'Bell' },
  { value: 'ChatDotRound', label: '讨论', icon: 'ChatDotRound' }
]

// 获取图标颜色（Jira风格）
const getIconColor = (typeKey) => {
  const colorMap = {
    'epic': '#6554C0',      // 紫色 - 史诗
    'story': '#0052CC',     // 蓝色 - 故事
    'task': '#4FADE6',      // 天蓝 - 任务
    'bug': '#FF5630',       // 红色 - 问题
    'subtask': '#7A869A'    // 灰色 - 子任务
  }
  return colorMap[typeKey] || '#909399'
}

// 获取图标标签
const getIconLabel = (typeKey) => {
  const labelMap = {
    'epic': '史诗',
    'story': '故事',
    'task': '任务',
    'bug': '问题',
    'subtask': '子任务'
  }
  return labelMap[typeKey] || ''
}

const rules = {
  typeKey: [
    { required: true, message: '请输入类型标识', trigger: 'blur' },
    { pattern: /^[a-z_]+$/, message: '只能包含小写字母和下划线', trigger: 'blur' }
  ],
  typeName: [
    { required: true, message: '请输入类型名称', trigger: 'blur' }
  ]
}

// 加载数据
const loadData = async () => {
  try {
    typeList.value = await getWorkItemTypes(selectedTenantId.value)
  } catch (error) {
    console.error('加载失败:', error)
  }
}

// 加载租户列表
const loadTenants = async () => {
  try {
    tenantList.value = await getTenants()
  } catch (error) {
    console.error('加载租户失败:', error)
  }
}

// 租户变化
const handleTenantChange = () => {
  // 保存当前租户ID到localStorage
  localStorage.setItem('currentTenantId', selectedTenantId.value)
  loadData()
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增工作项类型'
  formData.value = {
    typeKey: '',
    typeName: '',
    description: '',
    icon: 'Document',
    typeCategory: 'STANDARD',
    hierarchyLevel: 0
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑工作项类型'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该工作项类型吗？', '提示', {
      type: 'warning'
    })
    await deleteWorkItemType(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (isEdit.value) {
      await updateWorkItemType(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createWorkItemType(formData.value, selectedTenantId.value)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

onMounted(() => {
  loadTenants()
  // 从localStorage恢复租户ID
  const savedTenantId = localStorage.getItem('currentTenantId')
  if (savedTenantId) {
    selectedTenantId.value = parseInt(savedTenantId)
  }
  loadData()
})
</script>

<style scoped>
.config-container {
  max-width: 1400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.icon-selector {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  padding: 8px;
}

.icon-option {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 8px;
  border: 2px solid #dcdfe6;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  background-color: #fff;
}

.icon-option:hover {
  border-color: #409EFF;
  background-color: #ecf5ff;
  transform: translateY(-2px);
}

.icon-option.selected {
  border-color: #409EFF;
  background-color: #ecf5ff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.icon-label {
  margin-top: 6px;
  font-size: 12px;
  color: #606266;
  text-align: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
