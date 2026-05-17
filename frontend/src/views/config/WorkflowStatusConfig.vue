<template>
  <div class="config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>工作流状态配置</span>
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
            新增状态
          </el-button>
        </div>
      </template>

      <el-table :data="statusList" border stripe>
        <el-table-column prop="statusCode" label="状态编码" width="150" />
        <el-table-column prop="statusName" label="状态名称" width="150" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag :type="getCategoryType(row.category)">
              {{ getCategoryName(row.category) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="displayOrder" label="显示顺序" width="100" align="center" />
        <el-table-column prop="isActive" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="状态编码" prop="statusCode">
          <el-input v-model="formData.statusCode" placeholder="例如: OPEN, IN_PROGRESS" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="状态名称" prop="statusName">
          <el-input v-model="formData.statusName" placeholder="例如: 待处理, 进行中" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="选择分类" style="width: 100%">
            <el-option label="待处理 (TO_DO)" value="TO_DO" />
            <el-option label="进行中 (IN_PROGRESS)" value="IN_PROGRESS" />
            <el-option label="已完成 (DONE)" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="显示顺序" prop="displayOrder">
          <el-input-number v-model="formData.displayOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="isActive">
          <el-switch v-model="formData.isActive" />
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
import { getWorkflowStatuses, createWorkflowStatus, updateWorkflowStatus, deleteWorkflowStatus } from '@/api/workflowStatus'
import { getTenants } from '@/api/tenant'

const tenantList = ref([])
const selectedTenantId = ref(1)
const statusList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增工作流状态')
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref(null)

const formData = ref({
  statusCode: '',
  statusName: '',
  category: 'TO_DO',
  displayOrder: 0,
  isActive: true
})

const rules = {
  statusCode: [{ required: true, message: '请输入状态编码', trigger: 'blur' }],
  statusName: [{ required: true, message: '请输入状态名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }]
}

const getCategoryName = (category) => {
  const map = { TO_DO: '待处理', IN_PROGRESS: '进行中', DONE: '已完成' }
  return map[category] || category
}

const getCategoryType = (category) => {
  const map = { TO_DO: 'info', IN_PROGRESS: 'warning', DONE: 'success' }
  return map[category] || ''
}

const loadData = async () => {
  try {
    statusList.value = await getWorkflowStatuses(selectedTenantId.value)
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
  loadData()
}

const handleAdd = () => {
  isEdit.value = false
  currentId.value = null
  dialogTitle.value = '新增工作流状态'
  formData.value = {
    statusCode: '',
    statusName: '',
    category: 'TO_DO',
    displayOrder: 0,
    isActive: true
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  currentId.value = row.id
  dialogTitle.value = '编辑工作流状态'
  formData.value = {
    statusCode: row.statusCode,
    statusName: row.statusName,
    category: row.category,
    displayOrder: row.displayOrder,
    isActive: row.isActive
  }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除工作流状态 "${row.statusName}" 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteWorkflowStatus(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (isEdit.value) {
      // 更新
      const updateData = {
        statusName: formData.value.statusName,
        category: formData.value.category,
        displayOrder: formData.value.displayOrder,
        isActive: formData.value.isActive
      }
      await updateWorkflowStatus(currentId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      // 创建
      await createWorkflowStatus(formData.value, selectedTenantId.value)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadData()
  } catch (error) {
    if (error !== false) { // 排除表单验证失败
      console.error('提交失败:', error)
      ElMessage.error(error.response?.data?.message || '操作失败')
    }
  }
}

onMounted(() => {
  loadTenants()
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
</style>
