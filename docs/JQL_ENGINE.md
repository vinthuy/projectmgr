# JQL 查询引擎设计文档

## 1. 概述

### 1.1 目的
设计实现类Jira的JQL (Jira Query Language) 查询引擎，支持：
- 字段查询 (field queries)
- 操作符 (operators)
- 函数 (functions)
- 逻辑组合 (logical operators)
- 排序 (ordering)

### 1.2 设计目标
```
JQL语法示例:
project = PROJ AND status = "In Progress" ORDER BY created DESC

┌─────────────────────────────────────────────────────────────┐
│                     JQL Grammar                             │
├─────────────────────────────────────────────────────────────┤
│  query    → condition (AND condition)* (ORDER BY order)*  │
│             │                                               │
│  condition → field operator value                         │
│             │                                               │
│  operator  → = | != | > | < | >= | <= | IN | NOT IN | ~  │
│             │                                               │
│  value     → string | number | list | function()           │
│             │                                               │
│  order     → field (ASC | DESC)?                          │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. 数据模型

### 2.1 过滤器视图表

```sql
-- =============================================
-- JQL Filter Tables
-- =============================================

-- 过滤器/视图
DROP TABLE IF EXISTS filter_view CASCADE;
CREATE TABLE filter_view (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    jql_query TEXT NOT NULL,
    column_config JSONB,
    sort_config JSONB,
    group_by VARCHAR(100),
    is_shared BOOLEAN DEFAULT FALSE,
    is_favorite BOOLEAN DEFAULT FALSE,
    favorite_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_filter_jql ON filter_view(jql_query);
CREATE INDEX idx_filter_tenant ON filter_view(tenant_id);
CREATE INDEX idx_filter_favorite ON filter_view(is_favorite, favorite_count DESC);

-- 过滤器收藏
DROP TABLE IF EXISTS filter_favorite CASCADE;
CREATE TABLE filter_favorite (
    id BIGSERIAL PRIMARY KEY,
    filter_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(filter_id, user_id)
);
```

### 2.2 JQL历史表

```sql
-- JQL搜索历史
DROP TABLE IF EXISTS jql_search_history CASCADE;
CREATE TABLE jql_search_history (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    jql_query TEXT NOT NULL,
    result_count INTEGER,
    executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_jql_history_user ON jql_search_history(user_id, executed_at DESC);
```

---

## 3. 词法分析器

### 3.1 Token定义

```java
public enum JqlTokenType {
    // 关键字
    AND("and", "AND"),
    OR("or", "OR"),
    NOT("not", "NOT"),
    ORDER_BY("order by", "ORDER_BY"),
    BY("by", "BY"),
    ASC("asc", "ASCENDING"),
    DESC("desc", "DESCENDING"),
    
    // 操作符
    EQUALS("=", "EQUALS"),
    NOT_EQUALS("!=", "NOT_EQUALS"),
    GREATER_THAN(">", "GREATER_THAN"),
    LESS_THAN("<", "LESS_THAN"),
    GREATER_EQUALS(">=", "GREATER_EQUALS"),
    LESS_EQUALS("<=", "LESS_EQUALS"),
    IN("in", "IN"),
    NOT_IN("not in", "NOT_IN"),
    CONTAINS("~", "CONTAINS"),
    NOT_CONTAINS("!~", "NOT_CONTAINS"),
    IS("is", "IS"),
    IS_NOT("is not", "IS_NOT"),
    
    // 字面量
    IDENTIFIER("identifier", "IDENTIFIER"),
    STRING("string", "STRING"),
    NUMBER("number", "NUMBER"),
    KEYWORD("Jira keyword", "KEYWORD"),
    
    // 结束符
    EOF("end of file", "EOF");
}
```

### 3.2 词法分析器

```java
public class JqlLexer {
    
    private final String input;
    private int position = 0;
    private int line = 1;
    private int column = 1;
    
    /**
     * 词法分析 - 将输入字符串转换为Token流
     */
    public List<JqlToken> tokenize() {
        List<JqlToken> tokens = new ArrayList<>();
        
        while (position < input.length()) {
            skipWhitespace();
            
            if (position >= input.length()) break;
            
            char c = input.charAt(position);
            
            if (c == '"' || c == '\'') {
                tokens.add(readString(c));
            } else if (Character.isDigit(c)) {
                tokens.add(readNumber());
            } else if (Character.isLetter(c) || c == '_') {
                tokens.add(readIdentifierOrKeyword());
            } else if (isOperatorStart(c)) {
                tokens.add(readOperator());
            } else if (c == '(' || c == ')' || c == '[' || c == ']' 
                     || c == ',' || c == '"' || c == '\'') {
                tokens.add(new JqlToken(JqlTokenType.STRING, String.valueOf(c)));
                position++;
            } else {
                throw new JqlSyntaxError("Unexpected character: " + c, line, column);
            }
        }
        
        tokens.add(new JqlToken(JqlTokenType.EOF, null));
        return tokens;
    }
    
    private JqlToken readIdentifierOrKeyword() {
        StringBuilder sb = new StringBuilder();
        
        while (position < input.length() && 
               (Character.isLetterOrDigit(input.charAt(position)) || 
                input.charAt(position) == '_' ||
                input.charAt(position) == '.')) {
            sb.append(input.charAt(position));
            position++;
        }
        
        String value = sb.toString();
        
        // 检查是否是关键字
        JqlTokenType keyword = KEYWORDS.get(value.toLowerCase());
        if (keyword != null) {
            return new JqlToken(keyword, value);
        }
        
        return new JqlToken(JqlTokenType.IDENTIFIER, value);
    }
}
```

---

## 4. 语法分析器

### 4.1 语法规则

```java
public class JqlParser {
    
    private final JqlLexer lexer;
    private JqlToken currentToken;
    
    /**
     * JQL语法规则
     * 
     * query    → condition (AND condition)* (ORDER_BY order)*
     * condition → field operator value
     * operator → = | != | > | < | >= | <= | IN | NOT_IN | ~ | !~ | IS | IS_NOT
     * value → string | number | list | function | keyword
     * order → field (ASC | DESC)?
     */
    public JqlQuery parse() {
        List<JqlCondition> conditions = new ArrayList<>();
        List<JqlOrder> orders = new ArrayList<>();
        
        // 解析条件
        conditions.add(parseCondition());
        
        while (match(JqlTokenType.AND) || match(JqlTokenType.OR)) {
            JqlTokenType logicalOp = currentToken.getType();
            consume();
            conditions.add(parseCondition());
            // 记录逻辑运算符
        }
        
        // 解析排序
        if (match(JqlTokenType.ORDER_BY)) {
            consume();
            orders.add(parseOrder());
            
            while (match(JqlTokenType.COMMA)) {
                consume();
                orders.add(parseOrder());
            }
        }
        
        return new JqlQuery(conditions, orders);
    }
    
    private JqlCondition parseCondition() {
        // field
        JqlToken field = expect(JqlTokenType.IDENTIFIER);
        
        // operator
        JqlToken operator = expect(JqlTokenType.EQUALS, JqlTokenType.NOT_EQUALS, 
                               JqlTokenType.GREATER_THAN, JqlTokenType.LESS_THAN,
                               JqlTokenType.IN, JqlTokenType.NOT_IN,
                               JqlTokenType.CONTAINS, JqlTokenType.NOT_CONTAINS);
        
        // value
        Object value = parseValue();
        
        return new JqlCondition(field.getValue(), operator.getValue(), value);
    }
    
    private Object parseValue() {
        if (match(JqlTokenType.STRING)) {
            return currentToken.getValue();
        } else if (match(JqlTokenType.NUMBER)) {
            return Double.parseDouble(currentToken.getValue());
        } else if (match(JqlTokenType.IDENTIFIER)) {
            // 可能是函数或关键字
            if (peek().getType() == JqlTokenType.LPAREN) {
                return parseFunction();
            } else {
                return currentToken.getValue();
            }
        } else if (match(JqlTokenType.LPAREN)) {
            return parseList();
        } else {
            throw new JqlSyntaxError("Unexpected token: " + currentToken);
        }
    }
}
```

---

## 5. 查询执行器

### 5.1 查询构建

```java
public class JqlExecutor {
    
    /**
     * 执行JQL查询
     */
    public QueryResult execute(JqlQuery query, TenantContext ctx) {
        QueryDSLQuery<?> dslQuery = buildQuery(query, ctx);
        
        // 执行分页
        int total = dslQuery.fetchCount();
        List<WorkItem> items = dslQuery.fetch();
        
        return new QueryResult(items, total);
    }
    
    /**
     * 构建SQL查询
     */
    private QueryDSLQuery<?> buildQuery(JqlQuery query, TenantContext ctx) {
        QWorkItem qi = QWorkItem.workItem;
        
        QueryDSLQuery<?> dsl = new QueryDSLQuery<>(context, qi)
            .where(qi.tenantId.eq(ctx.getTenantId()))
            .where(qi.deleted.eq(false));
        
        // 添加条件
        for (JqlCondition condition : query.getConditions()) {
            dsl = addCondition(dsl, condition);
        }
        
        // 添加排序
        for (JqlOrder order : query.getOrders()) {
            dsl = addOrder(dsl, order);
        }
        
        return dsl;
    }
    
    private QueryDSLQuery<?> addCondition(QueryDSLQuery<?> dsl, JqlCondition cond) {
        String field = cond.getField();
        String operator = cond.getOperator();
        Object value = cond.getValue();
        
        switch (field) {
            case "project":
                return dsl.where(handleProjectOperator(operator, value));
            case "status":
                return dsl.where(handleStatusOperator(operator, value));
            case "assignee":
                return dsl.where(handleAssigneeOperator(operator, value));
            case "reporter":
                return dsl.where(handleReporterOperator(operator, value));
            case "priority":
                return dsl.where(handlePriorityOperator(operator, value));
            case "created":
            case "updated":
            case "dueDate":
                return dsl.where(handleDateOperator(field, operator, value));
            case "summary":
            case "description":
                return dsl.where(handleTextOperator(field, operator, value));
            default:
                // 自定义字段
                return dsl.where(handleCustomFieldOperator(field, operator, value));
        }
    }
    
    private BooleanExpression handleProjectOperator(String operator, Object value) {
        // 解析project key到id
        String projectKey = (String) value;
        Long projectId = projectService.getIdByKey(projectKey);
        
        switch (operator) {
            case "=":
                return QWorkItem.workItem.projectId.eq(projectId);
            case "!=":
                return QWorkItem.workItem.projectId.ne(projectId);
            case "IN":
                List<Long> ids = ((List) value).stream()
                    .map(k -> projectService.getIdByKey((String) k))
                    .collect(toList());
                return QWorkItem.workItem.projectId.in(ids);
            default:
                throw new JqlException("Unsupported operator for project: " + operator);
        }
    }
}
```

---

## 6. 字段支持

### 6.1 标准字段

| 字段名 | 类型 | 操作符 | 说明 |
|--------|------|--------|------|
| project | text | = != IN NOT IN | 项目 |
| key | text | = != ~ | 工作项Key |
| summary | text | ~ !~ = != | 摘要 |
| description | text | ~ !~ | 描述 |
| status | text | = != IN NOT IN | 状态 |
| priority | text | = != IN NOT IN | 优先级 |
| assignee | user | = != IS EMPTY | 负责人 |
| reporter | user | = != IS EMPTY | 报告人 |
| created | datetime | = != > < >= <= | 创建时间 |
| updated | datetime | = != > < >= <= | 更新时间 |
| duedate | datetime | = != > < >= <= IS EMPTY | 截止日期 |
| resolution | text | = != IS EMPTY | 解决方案 |
| labels | text | = IN | 标签 |
| components | text | = IN | 组件 |
| fixVersion | text | = IN | 修���版本 |

### 6.2 操作符支持

| 操作符 | 说明 | 适用类型 |
|--------|------|----------|
| = | 等于 | 所有 |
| != | 不等于 | 所有 |
| > | 大于 | datetime, number |
| < | 小于 | datetime, number |
| >= | 大于等于 | datetime, number |
| <= | 小于等于 | datetime, number |
| IN | 在列表中 | 所有 |
| NOT IN | 不在列表中 | 所有 |
| ~ | 包含 | text |
| !~ | 不包含 | text |
| IS | 是 | null相关 |
| IS NOT | 不是 | null相关 |

---

## 7. 函数支持

### 7.1 函数定义

```java
public class JqlFunctions {
    
    /**
     * JQL支持的内置函数
     */
    public static Map<String, JqlFunction> FUNCTIONS = new HashMap<>();
    
    static {
        // 用户相关函数
        FUNCTIONS.put("currentUser", new CurrentUserFunction());
        FUNCTIONS.put("loggedInUser", new LoggedInUserFunction());
        
        // 时间相关函数
        FUNCTIONS.put("now", new NowFunction());
        FUNCTIONS.put("today", new TodayFunction());
        FUNCTIONS.put("startOfDay", new StartOfDayFunction());
        FUNCTIONS.put("endOfDay", new EndOfDayFunction());
        FUNCTIONS.put("startOfWeek", new StartOfWeekFunction());
        FUNCTIONS.put("endOfWeek", new EndOfWeekFunction());
        FUNCTIONS.put("startOfMonth", new StartOfMonthFunction());
        FUNCTIONS.put("endOfMonth", new EndOfMonthFunction());
        
        // 项目相关函数
        FUNCTIONS.put("projectsLeadByUser", new ProjectsLeadByUserFunction());
        FUNCTIONS.put("projectsWhereUserHasPermission", new ProjectsPermissionFunction());
        
        // 链接相关函数
        FUNCTIONS.put("linkedIssues", new LinkedIssuesFunction());
        FUNCTIONS.put("subtasks", new SubtasksFunction());
        FUNCTIONS.put("children", new ChildrenFunction());
        FUNCTIONS.put("parent", new ParentFunction());
        
        // 搜索相关函数
        FUNCTIONS.put("issueHistory", new IssueHistoryFunction());
        FUNCTIONS.put("votedIssues", new VotedIssuesFunction());
        FUNCTIONS.put("watchedIssues", new WatchedIssuesFunction());
        FUNCTIONS.put("myAssignedIssues", new MyAssignedIssuesFunction());
        FUNCTIONS.put("myReportedIssues", new MyReportedIssuesFunction());
    }
}
```

### 7.2 函数实现

```java
public class CurrentUserFunction implements JqlFunction {
    
    @Override
    public String getName() {
        return "currentUser";
    }
    
    @Override
    public String getDescription() {
        return "Returns the current logged in user";
    }
    
    @Override
    public List<String> getParameters() {
        return Collections.emptyList();
    }
    
    @Override
    public Object evaluate(TenantContext ctx, User currentUser) {
        return currentUser.getUsername();
    }
}

public class NowFunction implements JqlFunction {
    
    @Override
    public String getName() {
        return "now";
    }
    
    @Override
    public String getDescription() {
        return "Returns the current date and time";
    }
    
    @Override
    public Object evaluate(TenantContext ctx, User currentUser) {
        return LocalDateTime.now();
    }
    
    @Override
    public String toSql(Object value) {
        // 转换为SQL: CURRENT_TIMESTAMP
        return "CURRENT_TIMESTAMP";
    }
}

// 时间计算函数示例
public class PlusDaysFunction implements JqlFunction {
    
    @Override
    public String getName() {
        return "plusDays";
    }
    
    @Override
    public List<String> getParameters() {
        return Arrays.asList("days");
    }
    
    @Override
    public Object evaluate(TenantContext ctx, User currentUser, Object... params) {
        int days = (Integer) params[0];
        return LocalDateTime.now().plusDays(days);
    }
}
```

---

## 8. SQL转换示例

### 8.1 简单查询

**JQL:**
```
project = PROJ
```

**SQL:**
```sql
SELECT * FROM work_item 
WHERE tenant_id = 1 
  AND deleted = false 
  AND project_id = (SELECT id FROM project WHERE tenant_id = 1 AND project_key = 'PROJ')
```

### 8.2 组合查询

**JQL:**
```
project = PROJ AND status = "In Progress"
```

**SQL:**
```sql
SELECT * FROM work_item 
WHERE tenant_id = 1 
  AND deleted = false 
  AND project_id = 1 
  AND status = 'IN_PROGRESS'
```

### 8.3 时间查询

**JQL:**
```
created >= -7d
```

**SQL:**
```sql
SELECT * FROM work_item 
WHERE tenant_id = 1 
  AND deleted = false 
  AND created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days'
```

### 8.4 函数查询

**JQL:**
```
assignee = currentUser() AND status = "To Do"
```

**SQL:**
```sql
SELECT * FROM work_item 
WHERE tenant_id = 1 
  AND deleted = false 
  AND assignee_user_id = :currentUserId 
  AND status = 'TODO'
```

### 8.5 文本搜索

**JQL:**
```
summary ~ "login error"
```

**SQL:**
```sql
SELECT * FROM work_item 
WHERE tenant_id = 1 
  AND deleted = false 
  AND summary ILIKE '%login error%'
```

---

## 9. API设计

### 9.1 查询API

| 方法 | 路径 | 说明 |
|-----|------|------|
| POST | /api/work-items/search | JQL搜索 |
| GET | /api/filters | 过滤器列表 |
| POST | /api/filters | 创建过滤器 |
| GET | /api/filters/{id} | 过滤器详情 |
| PUT | /api/filters/{id} | 更新过滤器 |
| DELETE | /api/filters/{id} | 删除过滤器 |
| GET | /api/filters/{id}/execute | 执行过滤器 |
| POST | /api/filters/{id}/favorite | 收藏过滤器 |
| DELETE | /api/filters/{id}/favorite | 取消收藏 |

### 9.2 请求示例

**JQL搜索:**
```json
POST /api/work-items/search
Request:
{
  "jql": "project = PROJ AND status = 'In Progress' ORDER BY created DESC",
  "startIndex": 0,
  "maxResults": 50
}

Response:
{
  "code": 200,
  "data": {
    "issues": [...],
    "total": 100,
    "startIndex": 0,
    "maxResults": 50
  }
}
```

**创建过滤器:**
```json
POST /api/filters
Request:
{
  "name": "My Open Issues",
  "description": "我的待办工作项",
  "jql": "assignee = currentUser() AND status = 'To Do' ORDER BY created DESC",
  "isShared": true
}
```

---

## 10. 验收标准

### 10.1 词法分析
- [ ] 正确识别关键字
- [ ] 正确识别操作符
- [ ] 正确识别字符串/数字
- [ ] 错误处理

### 10.2 语法分析
- [ ] 解析简单查询
- [ ] 解析AND/OR组合
- [ ] 解析ORDER BY

### 10.3 查询执行
- [ ] 支持project查询
- [ ] 支持status查询
- [ ] 支持用户字段查询
- [ ] 支持时间字段查询
- [ ] 支持自定义字段

### 10.4 函数
- [ ] currentUser()正确执行
- [ ] 时间函数正确计算
- [ ] 支持函数嵌套

### 10.5 场景测试

**场景1: 项目+状态查询**
```
project = PROJ AND status = "Open"
```

**场景2: 我的工作项**
```
assignee = currentUser() ORDER BY created DESC
```

**场景3: 最近7天创建的**
```
created >= -7d ORDER BY created DESC
```

**场景4: 包含关键字**
```
summary ~ "login" ORDER BY priority DESC
```

**场景5: 多条件组合**
```
project = PROJ AND status IN ("Open", "In Progress") AND priority IN ("High", "Highest")
```