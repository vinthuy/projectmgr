<template>
  <div class="screen-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>屏幕管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建屏幕
          </el-button>
        </div>
      </template>

      <!-- 说明 -->
      <el-alert
        title="屏幕（Screen）说明"
        type="info"
        :closable="false"
        style="margin-bottom: 20px"
      >
        <p>屏幕定义了在不同操作场景下显示的字段集合。可以为不同的问题类型配置不同的屏幕。</p>
        <p><strong>系统屏幕：</strong>不可删除，包含默认字段配置</p>
        <p><strong>自定义屏幕：</strong>可以创建、编辑、删除</p>
      </el-alert>

      <!-- Screen列表 -->
      <el-table :data="screenList" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="screenName" label="屏幕名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="250" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isSystem ? 'success' : 'info'">
              {{ row.isSystem ? '系统' : '自定义' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联Issue Type" min-width="200">
          <template #default="{ row }">
            <span v-if="row.issueTypeMappings && Object.keys(row.issueTypeMappings).length > 0">
              {{ Object.values(row.issueTypeMappings).join(', ') }}
            </span>
            <span v-else style="color: #999">未配置</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row.id)">
              详情
            </el-button>
            <el-button 
              link 
              type="primary" 
              size="small" 
              @click="handleEdit(row)"
              :disabled="row.isSystem"
            >
              编辑
            </el-button>
            <el-button 
              link 
              type="danger" 
              size="small" 
              @click="handleDelete(row)"
              :disabled="row.isSystem"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑屏幕' : '新建屏幕'"
      width="500px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="屏幕名称" prop="screenName">
          <el-input v-model="form.screenName" placeholder="请输入屏幕名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getScreens, createScreen, updateScreen, deleteScreen } from '@/api/screen'

const router = useRouter()

// 数据
const loading = ref(false)
const screenList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  id: null,
  screenName: '',
  description: ''
})

const rules = {
  screenName: [
    { required: true, message: '请输入屏幕名称', trigger: 'blur' }
  ]
}

// 加载Screen列表
const loadScreens = async () => {
  loading.value = true
  try {
    const res = await getScreens()
    screenList.value = res || []
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

// 查看详情
const handleViewDetail = (id) => {
  router.push(`/config/screens/${id}`)
}

// 新建
const handleCreate = () => {
  isEdit.value = false
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    screenName: row.screenName,
    description: row.description
  }
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除屏幕"${row.screenName}"吗？`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await deleteScreen(row.id)
    ElMessage.success('删除成功')
    loadScreens()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      if (isEdit.value) {
        await updateScreen(form.value.id, {
          screenName: form.value.screenName,
          description: form.value.description
        })
        ElMessage.success('更新成功')
      } else {
        await createScreen({
          screenName: form.value.screenName,
          description: form.value.description
        })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadScreens()
    } catch (error) {
      ElMessage.error('操作失败：' + error.message)
    } finally {
      submitting.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  form.value = {
    id: null,
    screenName: '',
    description: ''
  }
  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

onMounted(() => {
  loadScreens()
})
</script>

<style scoped>
.screen-list-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
