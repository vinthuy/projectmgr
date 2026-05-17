import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/Layout.vue'

const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' }
      },
      {
        path: 'work-items',
        name: 'WorkItems',
        component: () => import('@/views/WorkItemList.vue'),
        meta: { title: '工作项列表', icon: 'List' }
      },
      {
        path: 'config/tenants',
        name: 'Tenants',
        component: () => import('@/views/config/TenantConfig.vue'),
        meta: { title: '租户管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'config/types',
        name: 'WorkItemTypes',
        component: () => import('@/views/config/WorkItemTypeConfig.vue'),
        meta: { title: '工作项类型配置', icon: 'Setting' }
      },
      {
        path: 'config/statuses',
        name: 'WorkflowStatuses',
        component: () => import('@/views/config/WorkflowStatusConfig.vue'),
        meta: { title: '工作流状态配置', icon: 'Connection' }
      },
      {
        path: 'config/fields',
        name: 'FieldDefinitions',
        component: () => import('@/views/config/FieldDefinitionConfig.vue'),
        meta: { title: '字段定义配置', icon: 'Document' }
      },
      {
        path: 'config/link-types',
        name: 'IssueLinkTypes',
        component: () => import('@/views/config/IssueLinkTypeConfig.vue'),
        meta: { title: '关系类型配置', icon: 'Link' }
      },
      {
        path: 'config/screens',
        name: 'Screens',
        component: () => import('@/views/config/ScreenList.vue'),
        meta: { title: '屏幕管理', icon: 'Monitor' }
      },
      {
        path: 'config/screens/:id',
        name: 'ScreenDetail',
        component: () => import('@/views/config/ScreenDetail.vue'),
        meta: { title: '屏幕详情', icon: 'Monitor' }
      },
      {
        path: 'config/templates',
        name: 'ProjectTemplates',
        component: () => import('@/views/config/ProjectTemplateList.vue'),
        meta: { title: '项目模板管理', icon: 'Files' }
      },
      {
        path: 'config/templates/:id',
        name: 'ProjectTemplateConfig',
        component: () => import('@/views/config/ProjectTemplateConfig.vue'),
        meta: { title: '项目模板配置', icon: 'Files' }
      },
      {
        path: 'config/schemes',
        name: 'SchemeManagement',
        component: () => import('@/views/config/SchemeManagement.vue'),
        meta: { title: '方案管理', icon: 'Setting' }
      },
      {
        path: 'agent',
        name: 'AgentChat',
        component: () => import('@/views/agent/AgentChat.vue'),
        meta: { title: 'Agent智能助手', icon: 'ChatDotRound' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
