<template>
  <div class="agent-chat-container">
    <!-- 顶部导航栏 -->
    <div class="chat-header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="page-title">Agent智能助手</span>
        </template>
      </el-page-header>
      <div class="header-actions">
        <el-button :icon="BarChart" @click="showStats = !showStats">统计</el-button>
        <el-button :icon="Setting" @click="showSettings = true">设置</el-button>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="chat-body">
      <!-- 侧边栏 -->
      <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
        <!-- 会话历史 -->
        <div class="session-history">
          <h3>📋 会话历史</h3>
          <el-button 
            type="primary" 
            plain 
            size="small" 
            @click="createNewSession"
            style="width: 100%; margin-bottom: 10px"
          >
            ✨ 新对话
          </el-button>
          
          <div class="session-list">
            <div
              v-for="session in sessions"
              :key="session.id"
              class="session-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
            >
              <span>{{ session.name }}</span>
              <el-icon class="delete-icon" @click.stop="deleteSession(session.id)">
                <Close />
              </el-icon>
            </div>
          </div>
        </div>

        <!-- 上下文信息 -->
        <div class="context-info">
          <h3>ℹ️ 当前上下文</h3>
          <div class="info-item">
            <label>当前项目:</label>
            <span>{{ currentProject?.name || '未选择' }}</span>
          </div>
          <div class="info-item">
            <label>工作项总数:</label>
            <span>{{ stats.totalCount || 0 }}</span>
          </div>
        </div>
      </aside>

      <!-- 对话主区域 -->
      <main class="chat-main">
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div
            v-for="msg in messages"
            :key="msg.id"
            class="message-item"
            :class="msg.role"
          >
            <!-- AI消息 -->
            <div v-if="msg.role === 'assistant'" class="message-content">
              <el-avatar :size="32" class="avatar">🤖</el-avatar>
              <div class="message-bubble">
                <div class="message-text" v-html="renderMarkdown(msg.content)"></div>
                
                <!-- 工作项卡片 -->
                <div v-if="msg.workItem" class="work-item-card">
                  <div class="card-header">
                    <span class="card-title">📋 {{ msg.workItem.id }}: {{ msg.workItem.title }}</span>
                  </div>
                  <div class="card-body">
                    <div class="card-field">
                      <label>状态:</label>
                      <el-tag :type="getStatusType(msg.workItem.status)" size="small">
                        {{ getStatusText(msg.workItem.status) }}
                      </el-tag>
                    </div>
                    <div class="card-field">
                      <label>优先级:</label>
                      <el-tag :type="getPriorityType(msg.workItem.priority)" size="small">
                        {{ getPriorityText(msg.workItem.priority) }}
                      </el-tag>
                    </div>
                    <div class="card-field">
                      <label>负责人:</label>
                      <span>{{ msg.workItem.assignee || '未分配' }}</span>
                    </div>
                    <div class="card-field">
                      <label>截止日期:</label>
                      <span>{{ msg.workItem.dueDate || '无' }}</span>
                    </div>
                  </div>
                  <div class="card-actions">
                    <el-button size="small" @click="viewWorkItem(msg.workItem.id)">
                      查看详情
                    </el-button>
                    <el-button size="small" type="primary" @click="editWorkItem(msg.workItem.id)">
                      编辑
                    </el-button>
                  </div>
                </div>

                <!-- 建议操作 -->
                <div v-if="msg.suggestedActions && msg.suggestedActions.length > 0" class="suggested-actions">
                  <div class="suggestion-label">💡 你可以继续：</div>
                  <el-button
                    v-for="action in msg.suggestedActions"
                    :key="action.label"
                    size="small"
                    plain
                    @click="executeSuggestion(action)"
                  >
                    {{ action.label }}
                  </el-button>
                </div>
              </div>
            </div>

            <!-- 用户消息 -->
            <div v-else class="message-content">
              <div class="message-bubble user">
                <div class="message-text">{{ msg.content }}</div>
              </div>
              <el-avatar :size="32" class="avatar">👤</el-avatar>
            </div>
          </div>

          <!-- 加载中提示 -->
          <div v-if="isTyping" class="message-item assistant">
            <div class="message-content">
              <el-avatar :size="32" class="avatar">🤖</el-avatar>
              <div class="message-bubble typing">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="input-area">
          <!-- 快捷操作按钮 -->
          <div class="quick-actions">
            <el-button size="small" @click="fillTemplate('query')">🔍 查询</el-button>
            <el-button size="small" @click="fillTemplate('create')">➕ 创建</el-button>
            <el-button size="small" @click="fillTemplate('update')">✏️ 更新</el-button>
            <el-button size="small" @click="fillTemplate('analyze')">📊 分析</el-button>
          </div>

          <!-- 文本输入框 -->
          <div class="input-wrapper">
            <el-input
              v-model="userInput"
              type="textarea"
              :rows="1"
              :autosize="{ minRows: 1, maxRows: 5 }"
              placeholder="💬 输入自然语言指令..."
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.enter.shift.exact="userInput += '\n'"
            />
            <el-button
              type="primary"
              :icon="Promotion"
              :loading="loading"
              :disabled="!userInput.trim()"
              @click="sendMessage"
              class="send-btn"
            >
              发送
            </el-button>
          </div>
        </div>
      </main>
    </div>

    <!-- 设置对话框 -->
    <el-dialog v-model="showSettings" title="Agent设置" width="500px">
      <el-form label-width="120px">
        <el-form-item label="当前项目">
          <el-select v-model="currentProjectId" placeholder="选择项目">
            <el-option
              v-for="project in projects"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="API Token">
          <el-input v-model="apiToken" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSettings = false">取消</el-button>
        <el-button type="primary" @click="saveSettings">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { BarChart, Setting, Close, Promotion } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { sendMessage as sendAgentMessage, getSessions, deleteSession as deleteSessionAPI, getMessageHistory } from '@/api/agent'

const router = useRouter()
const md = new MarkdownIt()

// ========== 核心状态 ==========
const messages = ref([])
const currentSessionId = ref(null)
const isTyping = ref(false)
const userInput = ref('')
const loading = ref(false)
const sidebarCollapsed = ref(false)
const showSettings = ref(false)
const showStats = ref(false)

const sessions = ref([])
const currentProject = ref(null)
const currentProjectId = ref(null)
const apiToken = ref('')
const projects = ref([])
const stats = ref({})

const messageListRef = ref(null)

// ========== 初始化 ==========
onMounted(async () => {
  await loadSessions()
  if (sessions.value.length > 0) {
    switchSession(sessions.value[0].id)
  } else {
    createNewSession()
  }
  await loadProjects()
})

// ========== 数据加载 ==========
const loadSessions = async () => {
  try {
    const result = await getSessions()
    sessions.value = result.sessions || []
  } catch (error) {
    ElMessage.error('加载会话失败: ' + error.message)
  }
}

const loadProjects = async () => {
  // TODO: 从 API加载项目列表
  projects.value = [
    { id: 'PROJ-A', name: '项目A' },
    { id: 'PROJ-B', name: '项目B' }
  ]
}

// ========== 会话管理 ==========
const createNewSession = () => {
  const newSession = {
    id: `sess_${Date.now()}`,
    name: `新对话 ${sessions.value.length + 1}`
  }
  sessions.value.unshift(newSession)
  switchSession(newSession.id)
}

const switchSession = async (sessionId) => {
  currentSessionId.value = sessionId
  messages.value = []
  
  // 加载历史消息
  try {
    const history = await getMessageHistory(sessionId)
    if (history.messages && history.messages.length > 0) {
      messages.value = history.messages.map(msg => ({
        id: msg.messageId,
        role: msg.role,
        content: msg.content,
        workItem: msg.metadata?.workItem,
        suggestedActions: msg.metadata?.suggestedActions
      }))
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
  }
}

const deleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个会话吗？', '警告', { type: 'warning' })
    await deleteSessionAPI(sessionId)
    await loadSessions()
    
    if (currentSessionId.value === sessionId && sessions.value.length > 0) {
      switchSession(sessions.value[0].id)
    }
    
    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + error.message)
    }
  }
}

// ========== 消息发送 ==========
const sendMessage = async () => {
  if (!userInput.value.trim()) return
  
  const userMessage = {
    id: Date.now(),
    role: 'user',
    content: userInput.value
  }
  
  messages.value.push(userMessage)
  const currentUserInput = userInput.value
  userInput.value = ''
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()
  
  // 调用API
  loading.value = true
  isTyping.value = true
  
  try {
    const response = await sendAgentMessage({
      sessionId: currentSessionId.value,
      message: currentUserInput,
      metadata: {
        currentProject: currentProjectId.value
      }
    })
    
    const aiMessage = {
      id: Date.now() + 1,
      role: 'assistant',
      content: response.content,
      workItem: response.workItems?.[0],
      suggestedActions: response.suggestions
    }
    
    messages.value.push(aiMessage)
    
    // 更新上下文
    if (response.workItem) {
      updateContext(response.workItem)
    }
    
  } catch (error) {
    ElMessage.error('发送失败: ' + error.message)
  } finally {
    loading.value = false
    isTyping.value = false
    await nextTick()
    scrollToBottom()
  }
}

// ========== 辅助函数 ==========
const renderMarkdown = (text) => {
  return md.render(text)
}

const getStatusType = (status) => {
  const map = {
    'TODO': 'info',
    'IN_PROGRESS': 'warning',
    'DONE': 'success',
    'CLOSED': 'info'
  }
  return map[status] || ''
}

const getStatusText = (status) => {
  const map = {
    'TODO': '待办',
    'IN_PROGRESS': '进行中',
    'DONE': '已完成',
    'CLOSED': '已关闭'
  }
  return map[status] || status
}

const getPriorityType = (priority) => {
  const map = {
    'LOW': 'info',
    'MEDIUM': '',
    'HIGH': 'warning',
    'CRITICAL': 'danger'
  }
  return map[priority] || ''
}

const getPriorityText = (priority) => {
  const map = {
    'LOW': '低',
    'MEDIUM': '中',
    'HIGH': '高',
    'CRITICAL': '紧急'
  }
  return map[priority] || priority
}

const fillTemplate = (type) => {
  const templates = {
    query: '帮我查一下工作项 ',
    create: '创建一个任务：，优先级，分配给',
    update: '更新工作项 的',
    analyze: '分析一下当前项目的'
  }
  userInput.value = templates[type] || ''
}

const executeSuggestion = (action) => {
  // TODO: 执行建议操作
  console.log('Execute suggestion:', action)
}

const viewWorkItem = (id) => {
  // TODO: 跳转到工作项详情页
  console.log('View work item:', id)
}

const editWorkItem = (id) => {
  userInput.value = `更新工作项 ${id} 的`
}

const updateContext = (workItem) => {
  // TODO: 更新上下文信息
  console.log('Update context:', workItem)
}

const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const goBack = () => {
  router.back()
}

const saveSettings = async () => {
  // TODO: 保存设置到后端
  ElMessage.success('设置已保存')
  showSettings.value = false
}
</script>

<style scoped lang="scss">
.agent-chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  
  .chat-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 24px;
    background: #fff;
    border-bottom: 1px solid #dcdfe6;
    
    .page-title {
      font-size: 18px;
      font-weight: 600;
    }
    
    .header-actions {
      display: flex;
      gap: 10px;
    }
  }
  
  .chat-body {
    flex: 1;
    display: flex;
    overflow: hidden;
    
    .sidebar {
      width: 280px;
      background: #fff;
      border-right: 1px solid #dcdfe6;
      display: flex;
      flex-direction: column;
      padding: 16px;
      
      &.collapsed {
        width: 0;
        padding: 0;
        overflow: hidden;
      }
      
      h3 {
        font-size: 14px;
        font-weight: 600;
        margin: 0 0 12px 0;
        color: #303133;
      }
      
      .session-history {
        margin-bottom: 24px;
        
        .session-list {
          max-height: 300px;
          overflow-y: auto;
          
          .session-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 8px 12px;
            margin-bottom: 4px;
            border-radius: 4px;
            cursor: pointer;
            transition: all 0.2s;
            
            &:hover {
              background: #f5f7fa;
              
              .delete-icon {
                opacity: 1;
              }
            }
            
            &.active {
              background: #ecf5ff;
              color: #409eff;
            }
            
            .delete-icon {
              opacity: 0;
              transition: opacity 0.2s;
              
              &:hover {
                color: #f56c6c;
              }
            }
          }
        }
      }
      
      .context-info {
        .info-item {
          display: flex;
          justify-content: space-between;
          padding: 8px 0;
          border-bottom: 1px solid #ebeef5;
          font-size: 13px;
          
          label {
            color: #909399;
          }
          
          span {
            color: #303133;
            font-weight: 500;
          }
        }
      }
    }
    
    .chat-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      
      .message-list {
        flex: 1;
        overflow-y: auto;
        padding: 24px;
        
        .message-item {
          margin-bottom: 24px;
          
          &.user {
            .message-content {
              flex-direction: row-reverse;
              
              .message-bubble {
                background: #409eff;
                color: #fff;
                
                &.user {
                  background: #409eff;
                }
              }
            }
          }
          
          .message-content {
            display: flex;
            gap: 12px;
            align-items: flex-start;
            
            .avatar {
              flex-shrink: 0;
            }
            
            .message-bubble {
              max-width: 70%;
              padding: 12px 16px;
              background: #fff;
              border-radius: 8px;
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
              
              .message-text {
                font-size: 14px;
                line-height: 1.6;
                
                :deep(p) {
                  margin: 0 0 8px 0;
                  
                  &:last-child {
                    margin-bottom: 0;
                  }
                }
              }
              
              &.typing {
                display: flex;
                gap: 4px;
                padding: 16px;
                
                .typing-dot {
                  width: 8px;
                  height: 8px;
                  border-radius: 50%;
                  background: #909399;
                  animation: typing 1.4s infinite;
                  
                  &:nth-child(2) {
                    animation-delay: 0.2s;
                  }
                  
                  &:nth-child(3) {
                    animation-delay: 0.4s;
                  }
                }
              }
            }
          }
        }
        
        // 工作项卡片
        .work-item-card {
          margin-top: 12px;
          border: 1px solid #dcdfe6;
          border-radius: 8px;
          overflow: hidden;
          
          .card-header {
            padding: 12px 16px;
            background: #f5f7fa;
            border-bottom: 1px solid #dcdfe6;
            
            .card-title {
              font-weight: 600;
              font-size: 14px;
            }
          }
          
          .card-body {
            padding: 12px 16px;
            
            .card-field {
              display: flex;
              align-items: center;
              margin-bottom: 8px;
              font-size: 13px;
              
              &:last-child {
                margin-bottom: 0;
              }
              
              label {
                width: 70px;
                color: #909399;
                flex-shrink: 0;
              }
              
              span {
                color: #303133;
              }
            }
          }
          
          .card-actions {
            padding: 12px 16px;
            border-top: 1px solid #dcdfe6;
            display: flex;
            gap: 8px;
          }
        }
        
        // 建议操作
        .suggested-actions {
          margin-top: 12px;
          
          .suggestion-label {
            font-size: 12px;
            color: #909399;
            margin-bottom: 8px;
          }
          
          .el-button {
            margin-right: 8px;
            margin-bottom: 8px;
          }
        }
      }
      
      .input-area {
        padding: 16px 24px;
        background: #fff;
        border-top: 1px solid #dcdfe6;
        
        .quick-actions {
          margin-bottom: 12px;
          display: flex;
          gap: 8px;
        }
        
        .input-wrapper {
          display: flex;
          gap: 12px;
          align-items: flex-end;
          
          .el-textarea {
            flex: 1;
          }
          
          .send-btn {
            flex-shrink: 0;
          }
        }
      }
    }
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-10px);
  }
}
</style>
