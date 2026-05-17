# Agent智能助手模块

## 📖 简介

基于AgentScope Java框架开发的智能工作项助手,采用**ReAct模式**(反馈-动作-优化),支持通过自然语言对话方式对华为云CodeArts Req工作项进行增删改查操作。

## ✨ 核心特性

- 🤖 **自然语言交互**: 直接用中文对话操作工作项
- 🎯 **智能意图识别**: 自动识别查询/创建/更新/删除/分析5种意图
- 🔄 **ReAct循环**: Feedback(反馈) → Action(动作) → Optimize(优化)
- 📚 **上下文管理**: 支持多轮对话,记忆历史上下文
- 📊 **持续学习**: 收集用户反馈,不断优化响应质量
- 🛠️ **工具扩展**: 模块化设计,易于添加新工具

## 🏗️ 技术架构

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   Frontend  │────▶│  Controller  │────▶│   Service   │
│  (Vue 3)    │     │  (REST API)  │     │ (Business)  │
└─────────────┘     └──────────────┘     └──────┬──────┘
                                                │
                                          ┌─────▼──────┐
                                          │    Agent    │
                                          │  (ReAct)    │
                                          └─────┬──────┘
                                                │
                                    ┌───────────┴───────────┐
                                    │                       │
                              ┌─────▼──────┐        ┌──────▼─────┐
                              │  Tools     │        │  Context   │
                              │ (5 Tools)  │        │  Manager   │
                              └─────┬──────┘        └────────────┘
                                    │
                              ┌─────▼──────────┐
                              │ CodeArts Client│
                              └─────┬──────────┘
                                    │
                          ┌─────────▼──────────┐
                          │  Huawei Cloud API  │
                          └────────────────────┘
```

## 📦 快速开始

### 1. 环境准备

- Java 21+
- PostgreSQL 12+
- Node.js 16+
- Maven 3.6+

### 2. 配置CodeArts API

编辑 `src/main/resources/application.yml`:

```yaml
codearts:
  api:
    base-url: https://openapi.huaweicloud.com
    token: your-api-token      # 替换为实际Token
    project-id: your-project-id # 替换为实际Project ID
```

### 3. 启动后端

```bash
cd D:\code\java\meta
mvn clean package -DskipTests
java -jar target/workitem-system-1.0.0.jar
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

### 5. 访问应用

打开浏览器: http://localhost:5173

点击左侧菜单 **"Agent智能助手"**

## 💬 使用示例

### 查询工作项
```
👤 用户: 帮我查询项目PROJ的工作项

🤖 Agent: ✅ 找到 3 个工作项:

       1. [PROJ-123] 修复登录bug (已完成)
       2. [PROJ-124] 添加用户权限 (进行中)
       3. [PROJ-125] 优化性能 (待处理)

       [查看详情] [筛选状态] [导出列表]
```

### 创建工作项
```
👤 用户: 创建一个新任务,标题是"测试功能"

🤖 Agent: ✅ 工作项创建成功!

       ID: PROJ-126
       标题: 测试功能

       [继续创建] [返回列表]
```

### 更新工作项
```
👤 用户: 更新PROJ-123的状态为已完成

🤖 Agent: ✅ 工作项更新成功!

       [再次修改] [查看更新]
```

### 统计分析
```
👤 用户: 分析项目PROJ的统计

🤖 Agent: 📊 工作项分析报告

       总工作项数: 10
       已完成: 6
       进行中: 3
       完成率: 60%

       [查看详细统计] [生成图表]
```

## 📋 API接口

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 发送消息 | POST | `/api/agent/chat` | 发送对话消息 |
| 获取历史 | GET | `/api/agent/history` | 获取对话历史 |
| 提交反馈 | POST | `/api/agent/feedback` | 提交用户反馈 |
| 创建会话 | POST | `/api/agent/session` | 创建新会话 |
| 会话列表 | GET | `/api/agent/sessions` | 获取会话列表 |

### 请求示例

```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "查询项目PROJ的工作项",
    "metadata": {
      "tenantId": 1
    }
  }'
```

## 🗂️ 项目结构

```
meta/
├── docs/
│   └── AGENT_MODULE_DESIGN.md          # 详细设计文档
├── src/main/java/com/workitem/
│   ├── agent/                          # Agent核心层
│   │   ├── client/
│   │   │   └── CodeArtsReqClient.java  # CodeArts API客户端
│   │   ├── tool/
│   │   │   ├── QueryTool.java          # 查询工具
│   │   │   ├── CreateTool.java         # 创建工具
│   │   │   ├── UpdateTool.java         # 更新工具
│   │   │   ├── DeleteTool.java         # 删除工具
│   │   │   └── AnalyzeTool.java        # 分析工具
│   │   ├── IntentParser.java           # 意图解析器
│   │   ├── ToolExecutor.java           # 工具执行器
│   │   ├── ResponseOptimizer.java      # 响应优化器
│   │   ├── ContextManager.java         # 上下文管理器
│   │   └── WorkItemAgent.java          # Agent主控制器
│   ├── controller/
│   │   └── AgentController.java        # REST API控制器
│   ├── service/
│   │   └── AgentService.java           # 业务逻辑层
│   ├── entity/                         # 实体类(3个)
│   ├── mapper/                         # Mapper接口(3个)
│   └── dto/                            # DTO类(3个)
├── frontend/src/
│   ├── api/
│   │   └── agent.js                    # API封装
│   └── views/agent/
│       ├── AgentChat.vue               # 主聊天界面
│       └── components/
│           ├── MessageBubble.vue       # 消息气泡
│           └── WorkItemCard.vue        # 工作项卡片
├── AGENT_QUICK_START.md                # 快速启动指南
└── AGENT_DEVELOPMENT_SUMMARY.md        # 开发总结
```

## 🔧 扩展开发

### 添加新Tool

1. 实现Tool接口:
```java
public class CustomTool implements Tool {
    @Override
    public Object execute(Map<String, Object> params) {
        // 自定义逻辑
        return result;
    }
}
```

2. 在ToolExecutor中注册:
```java
tools.put("CUSTOM", new CustomTool(codeArtsClient));
```

3. 在IntentParser中添加意图识别规则:
```java
INTENT_PATTERNS.put("CUSTOM", Pattern.compile("(自定义|特殊).*(操作)"));
```

## 📊 数据库表

### agent_session
会话表,存储对话会话信息

### agent_message
消息表,存储每条对话消息

### agent_learning_log
学习日志表,记录每次交互用于优化

## 🎯 ReAct模式详解

### Feedback (反馈解析)
- 解析用户自然语言输入
- 提取关键信息(ID、状态、优先级等)
- 识别意图类型(QUERY/CREATE/UPDATE/DELETE/ANALYZE)
- 计算置信度

### Action (动作执行)
- 根据意图选择合适的Tool
- 调用CodeArts Req API
- 处理API响应和异常
- 返回执行结果

### Optimize (优化策略)
- 评估执行结果
- 格式化输出内容
- 生成建议操作
- 收集用户反馈

## 📈 监控指标

- API调用成功率
- 平均响应时间
- 意图识别准确率
- 用户满意度评分(1-5星)

## 🐛 常见问题

### Q: CodeArts API调用失败?
A: 检查API Token和Project ID是否正确配置

### Q: 意图识别不准确?
A: 使用更明确的表达,如"查询工作项"而非"看一下"

### Q: 前端无法连接后端?
A: 确认后端已启动(http://localhost:8080)

详见: `AGENT_QUICK_START.md`

## 📚 相关文档

- [详细设计文档](docs/AGENT_MODULE_DESIGN.md)
- [快速启动指南](AGENT_QUICK_START.md)
- [开发总结](AGENT_DEVELOPMENT_SUMMARY.md)

## 🚀 未来规划

### 短期
- [ ] 增强意图识别准确率
- [ ] 支持批量操作
- [ ] 优化错误提示

### 中期
- [ ] 集成LLM大语言模型
- [ ] 增加高级查询功能
- [ ] 性能优化(缓存、异步)

### 长期
- [ ] 多Agent协作
- [ ] 智能推荐
- [ ] 自动生成报告

## 📝 许可证

本项目仅供学习和研究使用。

## 👥 贡献

欢迎提交Issue和Pull Request!

---

**Made with ❤️ using AgentScope Java**
