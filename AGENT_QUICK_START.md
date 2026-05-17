# Agent模块快速启动指南

## 1. 概述

Agent模块是一个基于ReAct模式(反馈-动作-优化)的智能助手,支持通过自然语言对话方式对华为云CodeArts Req工作项进行增删改查操作。

## 2. 前置条件

### 2.1 环境要求
- Java 21+
- Maven 3.6+
- PostgreSQL 12+
- Node.js 16+ (前端)

### 2.2 CodeArts Req API配置
需要华为云CodeArts Req的API访问凭证:
- API Token
- Project ID

## 3. 数据库初始化

### 3.1 自动初始化
启动后端应用时会自动执行`schema.sql`,创建以下表:
- `agent_session`: 会话表
- `agent_message`: 消息表
- `agent_learning_log`: 学习日志表

### 3.2 手动检查
```sql
-- 检查表是否创建成功
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' AND table_name LIKE 'agent_%';
```

## 4. 配置CodeArts API

### 4.1 修改application.yml
编辑 `src/main/resources/application.yml`:

```yaml
codearts:
  api:
    base-url: https://openapi.huaweicloud.com
    token: your-actual-api-token  # 替换为实际Token
    project-id: your-project-id   # 替换为实际项目ID
```

### 4.2 或使用环境变量
```bash
export CODEARTS_API_TOKEN=your-actual-api-token
export CODEARTS_PROJECT_ID=your-project-id
```

## 5. 启动后端

### 5.1 Maven启动
```bash
cd D:\code\java\meta
mvn clean package -DskipTests
java -jar target/workitem-system-1.0.0.jar
```

### 5.2 IDE启动
直接运行 `WorkItemApplication.java`

### 5.3 验证启动
访问: http://localhost:8080/api/agent/chat (应返回401或参数错误,表示接口可用)

## 6. 启动前端

### 6.1 安装依赖
```bash
cd D:\code\java\meta\frontend
npm install
```

### 6.2 启动开发服务器
```bash
npm run dev
```

### 6.3 访问应用
打开浏览器: http://localhost:5173

点击左侧菜单 "Agent智能助手"

## 7. 功能测试

### 7.1 测试查询工作项

**用户输入:**
```
查询项目PROJ的工作项
```

**预期响应:**
```
✅ 找到 3 个工作项:

1. [PROJ-123] 修复登录bug (已完成)
2. [PROJ-124] 添加用户权限 (进行中)
3. [PROJ-125] 优化性能 (待处理)

[查看详情] [筛选状态] [导出列表]
```

### 7.2 测试创建工作项

**用户输入:**
```
创建一个新任务,标题是"测试功能"
```

**预期响应:**
```
✅ 工作项创建成功!

ID: PROJ-126
标题: 测试功能

[继续创建] [返回列表]
```

### 7.3 测试更新工作项

**用户输入:**
```
更新工作项PROJ-123的状态为已完成
```

**预期响应:**
```
✅ 工作项更新成功!

[再次修改] [查看更新]
```

### 7.4 测试删除工作项

**用户输入:**
```
删除工作项PROJ-126
```

**预期响应:**
```
✅ 工作项已删除
```

### 7.5 测试统计分析

**用户输入:**
```
分析项目PROJ的工作项统计
```

**预期响应:**
```
📊 工作项分析报告

总工作项数: 10
已完成: 6
进行中: 3
完成率: 60%

[查看详细统计] [生成图表]
```

## 8. API测试

### 8.1 使用curl测试

#### 创建会话并发送消息
```bash
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "查询工作项",
    "metadata": {
      "tenantId": 1
    }
  }'
```

#### 获取对话历史
```bash
curl "http://localhost:8080/api/agent/history?sessionId=sess_xxx&page=1&pageSize=20"
```

#### 提交反馈
```bash
curl -X POST http://localhost:8080/api/agent/feedback \
  -H "Content-Type: application/json" \
  -d '{
    "messageId": "msg_xxx",
    "score": 5,
    "comment": "回答很准确"
  }'
```

### 8.2 使用Postman测试

导入以下集合:

**Request 1: Chat**
- Method: POST
- URL: http://localhost:8080/api/agent/chat
- Body (JSON):
```json
{
  "message": "帮我查询项目PROJ的工作项",
  "metadata": {
    "tenantId": 1
  }
}
```

**Request 2: History**
- Method: GET
- URL: http://localhost:8080/api/agent/history?sessionId={sessionId}&page=1&pageSize=20

**Request 3: Feedback**
- Method: POST
- URL: http://localhost:8080/api/agent/feedback
- Body (JSON):
```json
{
  "messageId": "{messageId}",
  "score": 5,
  "comment": "很好"
}
```

## 9. 常见问题

### 9.1 CodeArts API调用失败

**问题:** 返回401或403错误

**解决:**
1. 检查API Token是否正确
2. 确认Project ID有效
3. 验证网络连接

### 9.2 意图识别不准确

**问题:** Agent无法正确理解用户意图

**解决:**
1. 使用更明确的表达,如"查询工作项"而非"看一下"
2. 提供更多信息,如项目名称、工作项ID
3. 查看日志中的intent解析结果

### 9.3 前端无法连接后端

**问题:** 前端发送请求超时

**解决:**
1. 确认后端已启动 (http://localhost:8080)
2. 检查前端代理配置 (vite.config.js)
3. 查看浏览器控制台错误信息

### 9.4 数据库表不存在

**问题:** 启动时报表不存在错误

**解决:**
1. 确认PostgreSQL已启动
2. 检查数据库连接配置
3. 手动执行schema.sql

## 10. 日志查看

### 10.1 后端日志
```bash
# 查看实时日志
tail -f logs/workitem.log

# 搜索Agent相关日志
grep "Agent" logs/workitem.log
```

### 10.2 关键日志位置
- 意图解析: `IntentParser.java`
- 工具执行: `ToolExecutor.java`
- API调用: `CodeArtsReqClient.java`

## 11. 性能优化建议

### 11.1 缓存策略
- 工作项列表缓存5分钟
- 统计数据缓存10分钟

### 11.2 并发控制
- 限制单个会话的请求频率
- 使用连接池管理API调用

### 11.3 数据库优化
- 为session_id、created_at添加索引
- 定期清理旧的学习日志

## 12. 扩展开发

### 12.1 添加新的Tool

1. 实现Tool接口:
```java
public class CustomTool implements Tool {
    @Override
    public Object execute(Map<String, Object> params) {
        // 自定义逻辑
    }
}
```

2. 在ToolExecutor中注册:
```java
tools.put("CUSTOM", new CustomTool(codeArtsClient));
```

3. 在IntentParser中添加意图识别规则

### 12.2 增强意图识别

可以集成NLP模型提升准确率:
- 使用HanLP进行中文分词
- 使用BERT进行意图分类
- 训练自定义模型

## 13. 监控和告警

### 13.1 关键指标
- API调用成功率
- 平均响应时间
- 意图识别准确率
- 用户满意度评分

### 13.2 告警规则
- API失败率 > 10%
- 响应时间 > 5秒
- 连续错误 > 5次

## 14. 下一步计划

### 短期优化
- [ ] 支持更多自然语言表达
- [ ] 增加批量操作功能
- [ ] 优化错误提示

### 长期规划
- [ ] 集成LLM大语言模型
- [ ] 支持多Agent协作
- [ ] 智能推荐相关工作项
- [ ] 自动生成工作报告

## 15. 技术支持

如遇问题,请查看:
1. 设计文档: `docs/AGENT_MODULE_DESIGN.md`
2. 后端日志: `logs/workitem.log`
3. 浏览器控制台: F12开发者工具

---

**祝使用愉快!** 🚀
