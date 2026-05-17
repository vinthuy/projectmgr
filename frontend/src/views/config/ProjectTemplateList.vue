<template>
  <div class="template-list-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>项目模板管理</span>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新建模板
          </el-button>
        </div>
      </template>
      
      <el-table :data="templates" v-loading="loading" border>
        <el-table-column prop="templateKey" label="模板Key" width="200" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-empty v-if="!loading && templates.length === 0" description="暂无模板数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const templates = ref([])

const loadData = async () => {
  loading.value = true
  try {
    // TODO: 调用API获取模板列表 - 暂时使用模拟数据
    ElMessage.info('项目模板列表API尚未实现，当前为演示数据')
    templates.value = [
      {
        id: 1,
        templateKey: 'software-development',
        templateName: '软件开发模板',
        description: '适用于软件研发项目的标准模板，包含史诗、故事、任务、问题等工作项类型'
      },
      {
        id: 2,
        templateKey: 'bug-tracking',
        templateName: 'Bug跟踪模板',
        description: '专注于缺陷跟踪和管理的模板'
      }
    ]
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  router.push('/config/templates/new')
}

const handleEdit = (row) => {
  router.push(`/config/templates/${row.id}`)
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除模板“${row.templateName}”吗？`, '警告', {
      type: 'warning'
    })
    
    // TODO: 调用删除API
    ElMessage.warning('删除功能尚未实现')
    // await deleteTemplate(row.id)
    // ElMessage.success('删除成功')
    // await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败：' + error.message)
    }
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.template-list-container {
  padding: 20px;
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 600;
  }
}
</style>
