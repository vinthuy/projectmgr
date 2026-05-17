# 工作项管理系统 - 前端

基于 Vue3 + Element Plus 的工作项管理系统前端配置界面。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Vue Router** - 官方路由管理器
- **Pinia** - Vue 状态管理
- **Axios** - HTTP 客户端

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 接口
│   │   ├── workItemType.js
│   │   ├── workflowStatus.js
│   │   └── fieldDefinition.js
│   ├── layout/           # 布局组件
│   │   └── Layout.vue
│   ├── router/           # 路由配置
│   │   └── index.js
│   ├── utils/            # 工具函数
│   │   └── request.js
│   ├── views/            # 页面视图
│   │   ├── config/       # 配置页面
│   │   │   ├── WorkItemTypeConfig.vue      # 工作项类型配置
│   │   │   ├── WorkflowStatusConfig.vue    # 工作流状态配置
│   │   │   └── FieldDefinitionConfig.vue   # 字段定义配置
│   │   ├── Dashboard.vue         # 仪表盘
│   │   └── WorkItemList.vue      # 工作项列表
│   ├── App.vue
│   └── main.js
├── index.html
├── package.json
└── vite.config.js
```

## 快速开始

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

## 功能模块

### 1. 工作项类型配置
- 查看系统预置的工作项类型（史诗、故事、任务、缺陷、子任务）
- 支持自定义新的工作项类型
- 配置类型的图标、颜色、层级等属性

### 2. 工作流状态配置
- 管理工作流状态（待处理、进行中、已解决等）
- 按分类组织状态（TO_DO、IN_PROGRESS、DONE）
- 配置状态的显示顺序

### 3. 字段定义配置
- 创建和管理自定义字段
- 支持多种字段类型（文本、数字、日期、下拉选择等）
- 配置字段的必填性、默认值、选项等属性

## API 代理配置

在 `vite.config.js` 中配置了后端 API 代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 8080 端口。

## 开发规范

### 组件命名
- 使用 PascalCase 命名组件文件
- 单文件组件包含 template、script、style 三部分

### API 调用
- 所有 API 调用封装在 `src/api/` 目录
- 使用统一的 request 工具处理请求

### 样式规范
- 使用 scoped 样式避免污染
- 优先使用 Element Plus 内置样式

## 后续扩展

- [ ] 工作项 CRUD 操作界面
- [ ] 动态表单生成器
- [ ] 工作流可视化编辑器
- [ ] 数据统计和报表
- [ ] 权限管理
- [ ] 项目管理（多项目隔离）

## 许可证

MIT
