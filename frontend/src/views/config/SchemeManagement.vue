<template>
  <div class="scheme-management">
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <!-- 类型方案 -->
      <el-tab-pane label="工作项类型方案" name="type">
        <SchemeList
          :schemes="typeSchemes"
          :loading="loading.type"
          @create="handleCreateTypeScheme"
          @edit="handleEditTypeScheme"
          @delete="handleDeleteTypeScheme"
        />
      </el-tab-pane>

      <!-- 屏幕方案 -->
      <el-tab-pane label="屏幕方案" name="screen">
        <SchemeList
          :schemes="screenSchemes"
          :loading="loading.screen"
          @create="handleCreateScreenScheme"
          @edit="handleEditScreenScheme"
          @delete="handleDeleteScreenScheme"
        />
      </el-tab-pane>

      <!-- 工作流方案 -->
      <el-tab-pane label="工作流方案" name="workflow">
        <SchemeList
          :schemes="workflowSchemes"
          :loading="loading.workflow"
          @create="handleCreateWorkflowScheme"
          @edit="handleEditWorkflowScheme"
          @delete="handleDeleteWorkflowScheme"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 配置对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      @close="handleDialogClose"
    >
      <TypeSchemeConfig
        v-if="activeTab === 'type' && currentScheme"
        :scheme="currentScheme"
        @save="handleSaveTypeScheme"
      />
      <ScreenSchemeConfig
        v-if="activeTab === 'screen' && currentScheme"
        :scheme="currentScheme"
        @save="handleSaveScreenScheme"
      />
      <WorkflowSchemeConfig
        v-if="activeTab === 'workflow' && currentScheme"
        :scheme="currentScheme"
        @save="handleSaveWorkflowScheme"
      />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import SchemeList from './components/SchemeList.vue'
import TypeSchemeConfig from './components/TypeSchemeConfig.vue'
import ScreenSchemeConfig from './components/ScreenSchemeConfig.vue'
import WorkflowSchemeConfig from './components/WorkflowSchemeConfig.vue'
import { getTypeSchemes, deleteTypeScheme } from '@/api/typeScheme'
import { getScreenSchemes, deleteScreenScheme } from '@/api/screenScheme'
import { getWorkflowSchemes, deleteWorkflowScheme } from '@/api/workflowScheme'

const activeTab = ref('type')
const loading = ref({
  type: false,
  screen: false,
  workflow: false
})

const typeSchemes = ref([])
const screenSchemes = ref([])
const workflowSchemes = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('')
const currentScheme = ref(null)

// 加载数据
const loadTypeSchemes = async () => {
  loading.value.type = true
  try {
    typeSchemes.value = await getTypeSchemes()
  } catch (error) {
    ElMessage.error('加载类型方案失败')
  } finally {
    loading.value.type = false
  }
}

const loadScreenSchemes = async () => {
  loading.value.screen = true
  try {
    screenSchemes.value = await getScreenSchemes()
  } catch (error) {
    ElMessage.error('加载屏幕方案失败')
  } finally {
    loading.value.screen = false
  }
}

const loadWorkflowSchemes = async () => {
  loading.value.workflow = true
  try {
    workflowSchemes.value = await getWorkflowSchemes()
  } catch (error) {
    ElMessage.error('加载工作流方案失败')
  } finally {
    loading.value.workflow = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'type' && typeSchemes.value.length === 0) {
    loadTypeSchemes()
  } else if (tab === 'screen' && screenSchemes.value.length === 0) {
    loadScreenSchemes()
  } else if (tab === 'workflow' && workflowSchemes.value.length === 0) {
    loadWorkflowSchemes()
  }
}

// CRUD操作
const handleCreateTypeScheme = () => {
  dialogTitle.value = '新建类型方案'
  currentScheme.value = {}
  dialogVisible.value = true
}

const handleEditTypeScheme = (scheme) => {
  dialogTitle.value = '编辑类型方案'
  currentScheme.value = { ...scheme }
  dialogVisible.value = true
}

const handleDeleteTypeScheme = async (scheme) => {
  try {
    await ElMessageBox.confirm(`确定删除"${scheme.schemeName}"?`, '警告', { type: 'warning' })
    await deleteTypeScheme(scheme.id)
    ElMessage.success('删除成功')
    loadTypeSchemes()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleCreateScreenScheme = () => {
  dialogTitle.value = '新建屏幕方案'
  currentScheme.value = {}
  dialogVisible.value = true
}

const handleEditScreenScheme = (scheme) => {
  dialogTitle.value = '编辑屏幕方案'
  currentScheme.value = { ...scheme }
  dialogVisible.value = true
}

const handleDeleteScreenScheme = async (scheme) => {
  try {
    await ElMessageBox.confirm(`确定删除"${scheme.schemeName}"?`, '警告', { type: 'warning' })
    await deleteScreenScheme(scheme.id)
    ElMessage.success('删除成功')
    loadScreenSchemes()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleCreateWorkflowScheme = () => {
  dialogTitle.value = '新建工作流方案'
  currentScheme.value = {}
  dialogVisible.value = true
}

const handleEditWorkflowScheme = (scheme) => {
  dialogTitle.value = '编辑工作流方案'
  currentScheme.value = { ...scheme }
  dialogVisible.value = true
}

const handleDeleteWorkflowScheme = async (scheme) => {
  try {
    await ElMessageBox.confirm(`确定删除"${scheme.schemeName}"?`, '警告', { type: 'warning' })
    await deleteWorkflowScheme(scheme.id)
    ElMessage.success('删除成功')
    loadWorkflowSchemes()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleSaveTypeScheme = async () => {
  // TypeSchemeConfig内部已处理保存,这里只需关闭对话框
  dialogVisible.value = false
  loadTypeSchemes()
}

const handleSaveScreenScheme = async (data) => {
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadScreenSchemes()
}

const handleSaveWorkflowScheme = async (data) => {
  ElMessage.success('保存成功')
  dialogVisible.value = false
  loadWorkflowSchemes()
}

const handleDialogClose = () => {
  currentScheme.value = null
}

onMounted(() => {
  loadTypeSchemes()
})
</script>

<style scoped lang="scss">
.scheme-management {
  padding: 20px;
}
</style>
