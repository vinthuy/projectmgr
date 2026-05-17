<template>
  <div class="config-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>字段定义配置</span>
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
            新增字段
          </el-button>
        </div>
      </template>

      <el-table :data="fieldList" border stripe>
        <el-table-column prop="fieldKey" label="字段标识" width="150" />
        <el-table-column prop="fieldName" label="字段名称" width="150" />
        <el-table-column prop="fieldType" label="字段类型" width="130">
          <template #default="{ row }">
            <el-tag size="small">{{ getFieldTypeName(row.fieldType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataType" label="数据类型" width="100" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip min-width="200" />
        <el-table-column prop="required" label="必填" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'" size="small">
              {{ row.required ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isSystem" label="系统字段" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.isSystem ? 'warning' : 'success'" size="small">
              {{ row.isSystem ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)" :disabled="row.isSystem">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)" :disabled="row.isSystem">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="字段标识" prop="fieldKey">
          <el-input v-model="formData.fieldKey" placeholder="例如: severity, component" :disabled="isEdit" />
          <div class="form-tip">唯一标识，只能包含小写字母和下划线</div>
        </el-form-item>
        <el-form-item label="字段名称" prop="fieldName">
          <el-input v-model="formData.fieldName" placeholder="例如: 严重程度, 组件" />
        </el-form-item>
        <el-form-item label="字段类型" prop="fieldType">
          <el-select v-model="formData.fieldType" placeholder="选择类型" style="width: 100%" @change="handleTypeChange">
            <el-option label="单行文本 (TEXT)" value="TEXT" />
            <el-option label="富文本 (RICHTEXT)" value="RICHTEXT" />
            <el-option label="数字 (NUMBER)" value="NUMBER" />
            <el-option label="布尔值 (BOOLEAN)" value="BOOLEAN" />
            <el-option label="日期时间 (DATE)" value="DATE" />
            <el-option label="用户选择 (USER)" value="USER" />
            <el-option label="单选下拉 (SELECT)" value="SELECT" />
            <el-option label="多选下拉 (MULTI_SELECT)" value="MULTI_SELECT" />
            <el-option label="标签 (LABELS)" value="LABELS" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="字段描述" />
        </el-form-item>
        <el-form-item label="是否必填" prop="required">
          <el-switch v-model="formData.required" />
        </el-form-item>
        <el-form-item v-if="showOptions" label="选项列表" prop="options">
          <el-input
            v-model="optionsText"
            type="textarea"
            :rows="4"
            placeholder="每行一个选项，例如:&#10;LOW&#10;MEDIUM&#10;HIGH&#10;CRITICAL"
          />
          <div class="form-tip">仅用于单选/多选下拉类型</div>
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
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getFieldDefinitions, createFieldDefinition, updateFieldDefinition, deleteFieldDefinition } from '@/api/fieldDefinition'
import { getTenants } from '@/api/tenant'

const tenantList = ref([])
const selectedTenantId = ref(1)
const fieldList = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增字段定义')
const isEdit = ref(false)
const formRef = ref(null)

const formData = ref({
  fieldKey: '',
  fieldName: '',
  fieldType: 'TEXT',
  dataType: 'text',
  description: '',
  required: false,
  defaultValue: null,
  options: null
})

const optionsText = ref('')

const showOptions = computed(() => {
  return ['SELECT', 'MULTI_SELECT'].includes(formData.value.fieldType)
})

const rules = {
  fieldKey: [
    { required: true, message: '请输入字段标识', trigger: 'blur' },
    { pattern: /^[a-z_]+$/, message: '只能包含小写字母和下划线', trigger: 'blur' }
  ],
  fieldName: [{ required: true, message: '请输入字段名称', trigger: 'blur' }],
  fieldType: [{ required: true, message: '请选择字段类型', trigger: 'change' }]
}

// 获取字段类型中文名称
const getFieldTypeName = (type) => {
  const typeMap = {
    'TEXT': '文本',
    'RICHTEXT': '富文本',
    'NUMBER': '数字',
    'BOOLEAN': '布尔',
    'DATE': '日期',
    'USER': '用户',
    'SELECT': '单选',
    'MULTI_SELECT': '多选',
    'LABELS': '标签'
  }
  return typeMap[type] || type
}

const loadData = async () => {
  try {
    fieldList.value = await getFieldDefinitions(selectedTenantId.value)
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
  dialogTitle.value = '新增字段定义'
  formData.value = {
    fieldKey: '',
    fieldName: '',
    fieldType: 'TEXT',
    dataType: 'text',
    description: '',
    required: false,
    defaultValue: null,
    options: null
  }
  optionsText.value = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  dialogTitle.value = '编辑字段定义'
  formData.value = { ...row }
  optionsText.value = Array.isArray(row.options) ? row.options.join('\n') : ''
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该字段定义吗？', '提示', {
      type: 'warning'
    })
    await deleteFieldDefinition(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

const handleTypeChange = () => {
  if (!showOptions.value) {
    formData.value.options = null
    optionsText.value = ''
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 处理选项
    if (showOptions.value && optionsText.value) {
      formData.value.options = optionsText.value.split('\n').filter(o => o.trim())
    } else {
      formData.value.options = null
    }
    
    if (isEdit.value) {
      await updateFieldDefinition(formData.value.id, formData.value)
      ElMessage.success('更新成功')
    } else {
      await createFieldDefinition(formData.value, selectedTenantId.value)
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

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
