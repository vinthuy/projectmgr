<template>
  <el-container style="height: 100vh;">
    <!-- 侧边栏 -->
    <el-aside width="220px">
      <div class="logo">
        <h2>工作项管理系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        @select="handleMenuSelect"
      >
        <!-- 项目作业管理系统 -->
        <el-sub-menu index="project">
          <template #title>
            <el-icon><Briefcase /></el-icon>
            <span>项目作业管理</span>
          </template>
          <el-menu-item index="/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/work-items">
            <el-icon><List /></el-icon>
            <span>工作项列表</span>
          </el-menu-item>
          <el-menu-item index="/agent">
            <el-icon><ChatDotRound /></el-icon>
            <span>Agent智能助手</span>
          </el-menu-item>
        </el-sub-menu>

        <!-- 后台管理系统 -->
        <el-sub-menu index="admin">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>后台管理</span>
          </template>
          <el-menu-item index="/config/tenants">
            <el-icon><OfficeBuilding /></el-icon>
            <span>租户管理</span>
          </el-menu-item>
          
          <!-- 工作项配置分组 -->
          <el-sub-menu index="workitem-config">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>工作项配置</span>
            </template>
            <el-menu-item index="/config/types">
              <el-icon><Grid /></el-icon>
              <span>类型配置</span>
            </el-menu-item>
            <el-menu-item index="/config/statuses">
              <el-icon><Connection /></el-icon>
              <span>状态配置</span>
            </el-menu-item>
            <el-menu-item index="/config/fields">
              <el-icon><Memo /></el-icon>
              <span>字段配置</span>
            </el-menu-item>
            <el-menu-item index="/config/link-types">
              <el-icon><Link /></el-icon>
              <span>关系配置</span>
            </el-menu-item>
            <el-menu-item index="/config/screens">
              <el-icon><Monitor /></el-icon>
              <span>屏幕管理</span>
            </el-menu-item>
            <el-menu-item index="/config/templates">
              <el-icon><Files /></el-icon>
              <span>项目模板</span>
            </el-menu-item>
            <el-menu-item index="/config/schemes">
              <el-icon><Setting /></el-icon>
              <span>方案管理</span>
            </el-menu-item>
          </el-sub-menu>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header>
        <div class="header-content">
          <h3>{{ currentTitle }}</h3>
        </div>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Briefcase, Setting, Odometer, List, OfficeBuilding, Document, Grid, Connection, Memo, Link, Monitor, Files, ChatDotRound } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title || '工作项管理系统')

const handleMenuSelect = (index) => {
  console.log('Menu clicked:', index)
  if (index && index !== route.path) {
    router.push(index)
  }
}
</script>

<style scoped>
.el-aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #2b3a4c;
}

.logo h2 {
  color: #fff;
  font-size: 18px;
  margin: 0;
}

.el-menu {
  border-right: none;
}

.el-header {
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  padding: 0 20px;
}

.header-content h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.el-main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>
