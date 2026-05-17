<template>
  <div class="workitem-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>工作项列表</span>
          <el-button type="primary">
            <el-icon><Plus /></el-icon>
            创建工作项
          </el-button>
        </div>
      </template>

      <el-alert
        title="提示"
        type="info"
        description="工作项管理功能正在开发中，当前仅展示配置功能。"
        show-icon
        :closable="false"
      />
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.workitem-list {
  max-width: 1400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
