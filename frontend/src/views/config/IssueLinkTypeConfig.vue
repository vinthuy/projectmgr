<template>
  <div class="config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>关系类型配置</span>
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
            新增关系类型
          </el-button>
        </div>
      </template>

      <el-table :data="linkTypeList" border stripe>
        <el-table-column prop="linkKey" label="关系标识" width="150" />
        <el-table-column prop="inwardName" label="内向名称" width="150">
          <template #default="{ row }">
            <el-tag type="info">{{ row.inwardName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="outwardName" label="外向名称" width="150">
          <template #default="{ row }">
            <el-tag type="success">{{ row.outwardName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="displayOrder" label="显示顺序" width="100" align="center" />
        <el-table-column prop="isSystem" label="系统内置" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isSystem ? 'warning' : 'info'">
              {{ row.isSystem ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)" :disabled="row.isSystem">
              编辑
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)" :disabled="row.isSystem">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-alert
          title="关系类型说明"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        >
          <p><strong>关系标识：</strong>唯一标识，如 "blocks", "relates"</p>
          <p><strong>内向名称：</strong>目标工作项指向源工作项的关系，如 "被阻塞"</p>
          <p><strong>外向名称：</strong>源工作项指向目标工作项的关系，如 "阻塞"</p>
          <p><strong>示例：</strong>工作项A "阻塞" 工作项B，则工作项B "被阻塞" by 工作项A</p>
        </el-alert>

        <el-form-item label="关系标识" prop="linkKey">
          <el-input 
            v-model="formData.linkKey" 
            placeholder="例如: blocks, relates, duplicates" 
            :disabled="isEdit"
          />
        </el-form-item>
        <el-form-item label="内向名称" prop="inwardName">
          <el-input 
            v-model="formData.inwardName" 
            placeholder="例如: 被阻塞, 相关于, 重复于" 
          />
        </el-form-item>
        <el-form-item label="外向名称" prop="outwardName">
          <el-input 
            v-model="formData.outwardName" 
            placeholder="例如: 阻塞, 相关, 重复" 
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="formData.description" 
            type="textarea" 
            :rows="3"
            placeholder="描述该关系类型的用途" 
          />
        </el-form-item>
        <el-form-item label="显示顺序" prop="displayOrder">
          <el-input-number v-model="formData.displayOrder" :min="0" />
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
import { getIssueLinkTypes, createIssueLinkType, updateIssueLinkType, deleteIssueLinkType } from '@/api/issueLinkType'
import { getTenants } from '@/api/tenant'

const tenantList = ref([])
const selectedTenantId = ref(1)
const linkTypeList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增关系类型')
const isEdit = ref(false)
const currentId = ref(null)
const formRef = ref(null)

const formData = ref({
  linkKey: '',
  inwardName: '',
  outwardName: '',
  description: '',
  displayOrder: 0
})

const rules = {
  linkKey: [{ required: true, message: '请输入关系标识', trigger: 'blur' }],
  inwardName: [{ required: true, message: '请输入内向名称', trigger: 'blur' }],
  outwardName: [{ required: true, message: '请输入外向名称', trigger: 'blur' }]
}

const loadData = async () => {
  try {
    linkTypeList.value = await getIssueLinkTypes(selectedTenantId.value)
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载数据失败')
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
  dialogTitle.value = '新增关系类型'
  formData.value = {
    linkKey: '',
    inwardName: '',
    outwardName: '',
    description: '',
    displayOrder: 0
  }
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (row.isSystem) {
    ElMessage.warning('系统内置关系类型不可编辑')
    return
  }
  
  isEdit.value = true
  currentId.value = row.id
  dialogTitle.value = '编辑关系类型'
  formData.value = {
    linkKey: row.linkKey,
    inwardName: row.inwardName,
    outwardName: row.outwardName,
    description: row.description || '',
    displayOrder: row.displayOrder
  }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  if (row.isSystem) {
    ElMessage.warning('系统内置关系类型不可删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除关系类型 "${row.linkKey}" 吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteIssueLinkType(row.id)
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
        inwardName: formData.value.inwardName,
        outwardName: formData.value.outwardName,
        description: formData.value.description,
        displayOrder: formData.value.displayOrder
      }
      await updateIssueLinkType(currentId.value, updateData)
      ElMessage.success('更新成功')
    } else {
      // 创建
      await createIssueLinkType(formData.value, selectedTenantId.value)
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
