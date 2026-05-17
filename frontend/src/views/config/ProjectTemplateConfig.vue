<template>
  <div class="template-config-container">
    <!-- 顶部操作栏 -->
    <div class="config-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="page-title">{{ isEdit ? '编辑项目模板' : '新建项目模板' }}</span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="config-body" v-loading="loading">
      <!-- 基础信息卡片 -->
      <el-card class="section-card">
        <template #header>
          <div class="card-header">
            <el-icon><Document /></el-icon>
            <span>📋 基础信息</span>
          </div>
        </template>
        
        <el-form :model="template" :rules="formRules" label-width="120px">
          <el-form-item label="模板Key" prop="templateKey">
            <el-input 
              v-model="template.templateKey" 
              placeholder="例如：software-development"
              :disabled="isEdit"
            />
            <div class="form-tip">唯一标识，只能包含小写字母、数字、连字符</div>
          </el-form-item>
          
          <el-form-item label="模板名称" prop="templateName">
            <el-input 
              v-model="template.templateName" 
              placeholder="例如：软件开发模板"
            />
          </el-form-item>
          
          <el-form-item label="模板描述" prop="description">
            <el-input 
              v-model="template.description" 
              type="textarea"
              :rows="3"
              placeholder="描述该模板的用途和特点"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 工作项类型方案配置 -->
      <el-card class="section-card">
        <template #header>
          <div class="card-header">
            <el-icon><Setting /></el-icon>
            <span>🔧 工作项类型方案</span>
          </div>
        </template>
        
        <el-form label-width="120px">
          <el-form-item label="方案">
            <el-select 
              v-model="template.typeSchemeId" 
              placeholder="请选择类型方案"
              style="width: 400px"
            >
              <el-option
                v-for="scheme in typeSchemes"
                :key="scheme.id"
                :label="scheme.schemeName"
                :value="scheme.id"
              >
                <span>{{ scheme.schemeName }}</span>
                <el-tag v-if="scheme.isSystem" size="small" type="info" style="margin-left: 8px">
                  系统
                </el-tag>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 工作流方案配置 -->
      <el-card class="section-card">
        <template #header>
          <div class="card-header">
            <el-icon><Connection /></el-icon>
            <span>🔄 工作流方案</span>
          </div>
        </template>
        
        <el-form label-width="120px">
          <el-form-item label="方案">
            <el-select 
              v-model="template.workflowSchemeId" 
              placeholder="请选择工作流方案"
              style="width: 400px"
            >
              <el-option
                v-for="scheme in workflowSchemes"
                :key="scheme.id"
                :label="scheme.schemeName"
                :value="scheme.id"
              >
                <span>{{ scheme.schemeName }}</span>
                <el-tag v-if="scheme.isSystem" size="small" type="info" style="margin-left: 8px">
                  系统
                </el-tag>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 屏幕方案配置 -->
      <el-card class="section-card">
        <template #header>
          <div class="card-header">
            <el-icon><Monitor /></el-icon>
            <span>🖥️ 屏幕方案（字段布局）</span>
          </div>
        </template>
        
        <el-form label-width="120px">
          <el-form-item label="方案">
            <el-select 
              v-model="template.screenSchemeId" 
              placeholder="请选择屏幕方案"
              style="width: 400px"
              @change="handleScreenSchemeChange"
            >
              <el-option
                v-for="scheme in screenSchemes"
                :key="scheme.id"
                :label="scheme.schemeName"
                :value="scheme.id"
              >
                <span>{{ scheme.schemeName }}</span>
                <el-tag v-if="scheme.isSystem" size="small" type="info" style="margin-left: 8px">
                  系统
                </el-tag>
              </el-option>
            </el-select>
          </el-form-item>
          
          <!-- 屏幕映射配置表 -->
          <el-form-item label="屏幕映射" v-if="selectedScreenScheme">
            <el-table 
              :data="selectedScreenScheme.mappings" 
              border
              style="width: 100%"
            >
              <el-table-column label="工作项类型" width="200">
                <template #default="{ row }">
                  <div class="type-cell">
                    <el-icon><component :is="row.issueTypeIcon || 'Document'" /></el-icon>
                    <span>{{ row.issueTypeName }}</span>
                  </div>
                </template>
              </el-table-column>
              
              <el-table-column label="创建/编辑屏幕" min-width="250">
                <template #default="{ row }">
                  <el-select 
                    v-model="row.createScreenId"
                    placeholder="选择屏幕"
                    @change="handleScreenMappingChange"
                  >
                    <el-option
                      v-for="screen in screens"
                      :key="screen.id"
                      :label="screen.screenName"
                      :value="screen.id"
                    >
                      <span>{{ screen.screenName }}</span>
                      <el-tag v-if="screen.isSystem" size="small" type="success" style="margin-left: 8px">
                        系统
                      </el-tag>
                    </el-option>
                  </el-select>
                  <el-link 
                    v-if="row.createScreenId"
                    :href="`/config/screens/${row.createScreenId}`"
                    target="_blank"
                    style="margin-left: 8px"
                  >
                    配置
                  </el-link>
                </template>
              </el-table-column>
              
              <el-table-column label="查看屏幕" min-width="250">
                <template #default="{ row }">
                  <el-select 
                    v-model="row.viewScreenId"
                    placeholder="选择屏幕"
                    @change="handleScreenMappingChange"
                  >
                    <el-option
                      v-for="screen in screens"
                      :key="screen.id"
                      :label="screen.screenName"
                      :value="screen.id"
                    >
                      <span>{{ screen.screenName }}</span>
                      <el-tag v-if="screen.isSystem" size="small" type="success" style="margin-left: 8px">
                        系统
                      </el-tag>
                    </el-option>
                  </el-select>
                  <el-link 
                    v-if="row.viewScreenId"
                    :href="`/config/screens/${row.viewScreenId}`"
                    target="_blank"
                    style="margin-left: 8px"
                  >
                    配置
                  </el-link>
                </template>
              </el-table-column>
            </el-table>
            
            <div class="form-tip" style="margin-top: 8px">
              💡 提示：点击"配置"链接可在新标签页打开Screen配置页面
            </div>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Setting, Connection, Monitor } from '@element-plus/icons-vue'
import { 
  getScreenSchemes,
  getScreenSchemeDetail,
  batchUpdateScreenSchemeMappings
} from '@/api/screenScheme'
import { getScreens } from '@/api/screen'

const route = useRoute()
const router = useRouter()

// ========== 核心状态 ==========
const templateId = ref(route.params.id)
const isEdit = computed(() => !!templateId.value)

const template = ref({
  templateKey: '',
  templateName: '',
  description: '',
  typeSchemeId: null,
  workflowSchemeId: null,
  screenSchemeId: null
})

// 方案列表
const typeSchemes = ref([])
const workflowSchemes = ref([])
const screenSchemes = ref([])

// 方案详情
const selectedScreenScheme = ref(null)

// 屏幕列表
const screens = ref([])

// UI状态
const loading = ref(false)
const saving = ref(false)
const isDirty = ref(false)

// ========== 计算属性 ==========

/**
 * 表单验证规则
 */
const formRules = {
  templateKey: [
    { required: true, message: '请输入模板Key', trigger: 'blur' },
    { pattern: /^[a-z0-9-]+$/, message: '只能包含小写字母、数字、连字符', trigger: 'blur' }
  ],
  templateName: [
    { required: true, message: '请输入模板名称', trigger: 'blur' },
    { max: 200, message: '长度不能超过200字符', trigger: 'blur' }
  ],
  description: [
    { max: 1000, message: '长度不能超过1000字符', trigger: 'blur' }
  ]
}

// ========== 数据加载 ==========

/**
 * 加载模板详情和方案列表
 */
const loadData = async () => {
  loading.value = true
  try {
    const [
      screenSchemesRes,
      screensRes
    ] = await Promise.all([
      getScreenSchemes(),
      getScreens()
    ])
    
    screenSchemes.value = screenSchemesRes || []
    screens.value = screensRes || []
    
    // 临时使用模拟数据(待后续实现TypeScheme和WorkflowScheme)
    typeSchemes.value = [
      { id: 1, schemeName: '默认类型方案', isSystem: true }
    ]
    workflowSchemes.value = [
      { id: 1, schemeName: '标准工作流方案', isSystem: true }
    ]
    
    // 如果是编辑模式，加载模板详情
    if (isEdit.value && templateId.value !== 'new') {
      // TODO: 调用getTemplateById API - 暂时使用模拟数据
      ElMessage.warning('项目模板CRUD API尚未实现，当前仅演示Screen Scheme配置功能')
      template.value = {
        templateKey: 'software-development',
        templateName: '软件开发模板',
        description: '适用于软件研发项目的标准模板',
        typeSchemeId: 1,
        workflowSchemeId: 1,
        screenSchemeId: screenSchemesRes?.[0]?.id || null
      }
      
      // 加载方案详情
      if (template.value.screenSchemeId) {
        await loadScreenSchemeDetail(template.value.screenSchemeId)
      }
    } else {
      // 新建模式，默认选择第一个屏幕方案
      if (screenSchemesRes && screenSchemesRes.length > 0) {
        template.value.screenSchemeId = screenSchemesRes[0].id
        await loadScreenSchemeDetail(screenSchemesRes[0].id)
      }
    }
  } catch (error) {
    ElMessage.error('加载失败：' + error.message)
    console.error('Load data error:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载屏幕方案详情
 */
const loadScreenSchemeDetail = async (schemeId) => {
  try {
    selectedScreenScheme.value = await getScreenSchemeDetail(schemeId)
  } catch (error) {
    ElMessage.error('加载方案详情失败：' + error.message)
  }
}

// ========== 事件处理 ==========

/**
 * 方案选择变化
 */
const handleScreenSchemeChange = async (schemeId) => {
  if (schemeId) {
    await loadScreenSchemeDetail(schemeId)
  } else {
    selectedScreenScheme.value = null
  }
  isDirty.value = true
}

/**
 * 屏幕映射变化
 */
const handleScreenMappingChange = () => {
  isDirty.value = true
}

/**
 * 保存模板
 */
const handleSave = async () => {
  saving.value = true
  try {
    // 验证必填字段
    if (!template.value.templateKey || !template.value.templateName) {
      ElMessage.warning('请填写模板Key和模板名称')
      saving.value = false
      return
    }
    
    // 保存屏幕方案映射
    if (template.value.screenSchemeId && selectedScreenScheme.value?.mappings) {
      const mappings = selectedScreenScheme.value.mappings.map(m => ({
        issueTypeId: m.issueTypeId,
        createScreenId: m.createScreenId,
        editScreenId: m.editScreenId,
        viewScreenId: m.viewScreenId
      }))
      
      await batchUpdateScreenSchemeMappings(template.value.screenSchemeId, mappings)
      ElMessage.success('屏幕方案映射保存成功')
    }
    
    // TODO: 保存项目模板基础信息(需要实现ProjectTemplate API)
    if (!isEdit.value) {
      ElMessage.info('注意: 项目模板基础信息的创建/更新API尚未实现')
    } else {
      ElMessage.success('更新成功')
    }
    
    isDirty.value = false
  } catch (error) {
    ElMessage.error('保存失败：' + error.message)
    console.error('Save error:', error)
  } finally {
    saving.value = false
  }
}

/**
 * 返回上一页
 */
const goBack = () => {
  if (isDirty.value) {
    ElMessageBox.confirm('有未保存的更改，确定要离开吗？', '警告', {
      type: 'warning'
    }).then(() => {
      router.push('/config/templates')
    }).catch(() => {})
  } else {
    router.push('/config/templates')
  }
}

// ========== 生命周期 ==========
onMounted(() => {
  loadData()
})

// 监听表单变化
watch(template, () => {
  isDirty.value = true
}, { deep: true })
</script>

<style scoped lang="scss">
.template-config-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  
  // ========== 顶部操作栏 ==========
  .config-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 20px;
    height: 60px;
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
    
    .page-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  // ========== 主体内容区 ==========
  .config-body {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    
    .section-card {
      margin-bottom: 20px;
      
      .card-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        
        .el-icon {
          font-size: 18px;
          color: #409eff;
        }
      }
    }
    
    .form-tip {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
    
    .type-cell {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .el-icon {
        font-size: 16px;
        color: #409eff;
      }
    }
  }
}
</style>
