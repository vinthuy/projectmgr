<template>
  <div :class="['message-bubble', message.role]">
    <div class="bubble-content">
      <!-- 用户消息 -->
      <div v-if="message.role === 'user'" class="user-message">
        <span class="avatar">👤</span>
        <div class="content">{{ message.content }}</div>
      </div>
      
      <!-- 助手消息 -->
      <div v-else class="assistant-message">
        <span class="avatar">🤖</span>
        <div class="content-wrapper">
          <div class="content" style="white-space: pre-wrap;">{{ message.content }}</div>
          
          <!-- 工作项列表 -->
          <div v-if="message.workItems && message.workItems.length" class="work-items">
            <WorkItemCard 
              v-for="item in message.workItems" 
              :key="item.id"
              :item="item"
            />
          </div>
          
          <!-- 建议操作 -->
          <div v-if="message.suggestions && message.suggestions.length" class="suggestions">
            <el-tag
              v-for="suggestion in message.suggestions"
              :key="suggestion"
              size="small"
              effect="plain"
              class="suggestion-chip"
              @click="$emit('suggestion-click', suggestion)"
            >
              {{ suggestion }}
            </el-tag>
          </div>
          
          <!-- 反馈评分 -->
          <div class="feedback">
            <el-rate
              v-model="feedbackScore"
              :max="5"
              show-score
              text-color="#ff9900"
              score-template="{value}"
              @change="(value) => $emit('feedback', message.messageId, value)"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import WorkItemCard from './WorkItemCard.vue'

const props = defineProps({
  message: {
    type: Object,
    required: true
  }
})

defineEmits(['feedback', 'suggestion-click'])

const feedbackScore = ref(0)
</script>

<style scoped>
.message-bubble {
  display: flex;
  margin-bottom: 16px;
}

.message-bubble.user {
  justify-content: flex-end;
}

.bubble-content {
  max-width: 70%;
}

.user-message, .assistant-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.avatar {
  font-size: 28px;
  flex-shrink: 0;
}

.content {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f5f5f5;
  line-height: 1.6;
  color: #303133;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.user-message .content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.content-wrapper {
  flex: 1;
  background: white;
  padding: 16px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.work-items {
  margin-top: 16px;
}

.suggestions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.suggestion-chip {
  cursor: pointer;
  transition: all 0.3s;
}

.suggestion-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.feedback {
  margin-top: 12px;
  text-align: right;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}
</style>
