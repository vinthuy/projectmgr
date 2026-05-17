<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #409EFF;">
              <el-icon :size="30"><Document /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">156</div>
              <div class="stat-label">总工作项</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #E6A23C;">
              <el-icon :size="30"><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">42</div>
              <div class="stat-label">进行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #67C23A;">
              <el-icon :size="30"><CircleCheck /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">98</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" style="background: #F56C6C;">
              <el-icon :size="30"><WarningFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">16</div>
              <div class="stat-label">待处理</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>欢迎使用工作项管理系统</span>
      </template>
      <div class="welcome-content">
        <h3>系统功能</h3>
        <ul>
          <li>✅ 工作项管理 - 创建、查询、更新、删除工作项</li>
          <li>✅ 类型自定义 - 配置工作项类型（任务、缺陷、故事等）</li>
          <li>✅ 状态自定义 - 配置工作流状态（待处理、进行中等）</li>
          <li>✅ 字段自定义 - 动态添加和管理自定义字段</li>
          <li>✅ 动态查询 - 支持灵活的列选择和条件筛选</li>
        </ul>
        
        <h3 style="margin-top: 20px;">快速开始</h3>
        <p>1. 在左侧菜单选择"工作项类型配置"，查看和管理工作项类型</p>
        <p>2. 选择"工作流状态配置"，定义您的工作流程状态</p>
        <p>3. 选择"字段定义配置"，创建适合您业务的自定义字段</p>
        <p>4. 在"工作项列表"中管理和跟踪您的工作项</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 15px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.welcome-content {
  line-height: 1.8;
}

.welcome-content h3 {
  margin-bottom: 10px;
  color: #303133;
}

.welcome-content ul {
  padding-left: 20px;
}

.welcome-content li {
  margin: 8px 0;
  color: #606266;
}

.welcome-content p {
  margin: 8px 0;
  color: #606266;
}
</style>
