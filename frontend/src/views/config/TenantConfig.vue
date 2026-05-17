<template>
  <div class="tenant-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>租户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增租户
          </el-button>
        </div>
      </template>

      <el-table :data="tenantList" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="tenantKey" label="租户标识" width="150" />
        <el-table-column prop="tenantName" label="租户名称" width="200" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="licenseType" label="许可类型" width="120" align="center" />
        <el-table-column prop="maxUsers" label="最大用户数" width="100" align="center" />
        <el-table-column prop="maxProjects" label="最大项目数" width="100" align="center" />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button 
              link 
              type="danger" 
              size="small" 
              @click="handleDelete(row)"
              :disabled="row.tenantKey === 'default'"
            >
              删除
            </el-button>
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
        label-width="120px"
      >
        <el-form-item label="租户标识" prop="tenantKey">
          <el-input
            v-model="formData.tenantKey"
            placeholder="例如: company-a, team-b"
            :disabled="isEdit"
          />
          <div class="form-tip">只能包含小写字母、数字、下划线和横线，且必须以字母开头</div>
        </el-form-item>
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="formData.tenantName" placeholder="例如: A公司, B团队" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="租户描述"
          />
        </el-form-item>
        <el-form-item label="许可类型" prop="licenseType">
          <el-select v-model="formData.licenseType" placeholder="选择许可类型" style="width: 100%">
            <el-option label="免费版 (FREE)" value="FREE" />
            <el-option label="标准版 (STANDARD)" value="STANDARD" />
            <el-option label="企业版 (ENTERPRISE)" value="ENTERPRISE" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大用户数" prop="maxUsers">
          <el-input-number
            v-model="formData.maxUsers"
            :min="1"
            :max="1000"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大项目数" prop="maxProjects">
          <el-input-number
            v-model="formData.maxProjects"
            :min="1"
            :max="100"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="isEdit" label="状态" prop="status">
          <el-select v-model="formData.status" placeholder="选择状态" style="width: 100%">
            <el-option label="激活 (ACTIVE)" value="ACTIVE" />
            <el-option label="未激活 (INACTIVE)" value="INACTIVE" />
            <el-option label="已暂停 (SUSPENDED)" value="SUSPENDED" />
          </el-select>
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
import { getTenants, createTenant, updateTenant, deleteTenant } from '@/api/tenant'

const tenantList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增租户')
const isEdit = ref(false)
const formRef = ref(null)

const formData = ref({
  tenantKey: '',
  tenantName: '',
  description: '',
  licenseType: 'FREE',
  maxUsers: 10,
  maxProjects: 5,
  status: 'ACTIVE'
})

const rules = {
  tenantKey: [
    { required: true, message: '请输入租户标识', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_-]*$/, message: '只能包含小写字母、数字、下划线和横线，且必须以字母开头', trigger: 'blur' }
  ],
  tenantName: [
    { required: true, message: '请输入租户名称', trigger: 'blur' }
  ]
}

// 加载数据
const loadData = async () => {
  try {
    tenantList.value = await getTenants()
  } catch (error) {
    console.error('加载失败:', error)
  }
}

// 获取状态类型
const getStatusType = (status) => {
  const map = { ACTIVE: 'success', INACTIVE: 'info', SUSPENDED: 'warning' }
  return map[status] || ''
}

// 获取状态文本
const getStatusText = (status) => {
  const map = { ACTIVE: '激活', INACTIVE: '未激活', SUSPENDED: '已暂停' }
  return map[status] || status
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  dialogTitle.value = '新增租户'
  formData.value = {
    tenantKey: '',
    tenantName: '',
    description: '',
    licenseType: 'FREE',
    maxUsers: 10,
    maxProjects: 5,
    status: 'ACTIVE'
  }
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑租户'
  formData.value = { ...row }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  if (row.tenantKey === 'default') {
    ElMessage.warning('默认租户不能删除')
    return
  }
  
  try {
    await ElMessageBox.confirm('确定要删除该租户吗？删除后无法恢复！', '提示', {
      type: 'warning'
    })
    await deleteTenant(row.id)
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
      await updateTenant(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createTenant(formData.value)
      ElMessage.success('创建成功')
    }
    
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败:', error)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.tenant-container {
  max-width: 1400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
