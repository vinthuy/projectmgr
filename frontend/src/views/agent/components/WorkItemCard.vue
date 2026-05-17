<template>
  <el-card class="work-item-card" shadow="hover">
    <div class="card-header">
      <el-tag :type="getStatusType(item.status)" size="small">
        {{ item.status || '未知' }}
      </el-tag>
      <span class="item-id">{{ item.id }}</span>
    </div>
    
    <div class="card-body">
      <h4 class="item-title">{{ item.title || '无标题' }}</h4>
      <p v-if="item.description" class="item-description">
        {{ item.description }}
      </p>
    </div>
    
    <div class="card-footer">
      <el-tag v-if="item.priority" size="small" effect="plain">
        优先级: {{ item.priority }}
      </el-tag>
      <el-tag v-if="item.type" size="small" effect="plain">
        类型: {{ item.type }}
      </el-tag>
    </div>
  </el-card>
</template>

<script setup>
defineProps({
  item: {
    type: Object,
    required: true
  }
})

const getStatusType = (status) => {
  const statusMap = {
    'TODO': 'info',
    'IN_PROGRESS': 'warning',
    'DONE': 'success',
    'OPEN': 'primary'
  }
  return statusMap[status] || ''
}
</script>

<style scoped>
.work-item-card {
  margin-bottom: 12px;
  transition: all 0.3s;
}

.work-item-card:hover {
  transform: translateX(4px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.item-id {
  font-family: monospace;
  color: #909399;
  font-size: 12px;
}

.card-body {
  margin-bottom: 12px;
}

.item-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #303133;
}

.item-description {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.5;
}

.card-footer {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
