# PLAN.md - 开发计划

## 1. 项目概述

### 1.1 项目信息
| 项目 | 内容 |
|------|------|
| 项目名称 | Work Item Management System |
| 项目代号 | WIMS |
| 工期 | 5天 |
| 团队规模 | 1人 |

### 1.2 里程碑

| 里程碑 | 日期 | 交付物 |
|--------|------|--------|
| M1 - 基础框架 | Day 1 | 项目结构、配置、数据库 |
| M2 - 核心功能 | Day 2-3 | 实体、Mapper、Service |
| M3 - API层 | Day 3-4 | Controller、DTO、异常处理 |
| M4 - 测试验证 | Day 5 | 单元测试、集成验证 |

## 2. 开发任务分解

### 2.1 阶段一：基础框架搭建 (Day 1)

#### T1.1 项目初始化
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T1.1.1 | 创建Maven项目，配置pom.xml | 0.5h |
| T1.1.2 | 配置application.yml | 0.5h |
| T1.1.3 | 创建主应用类 | 0.25h |

#### T1.2 数据库设计
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T1.2.1 | 编写schema.sql建表脚本 | 0.5h |
| T1.2.2 | 创建索引和初始化数据 | 0.25h |

#### T1.3 配置文件
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T1.3.1 | MyBatis Plus配置类 | 0.5h |
| T1.3.2 | 全局异常处理 | 0.5h |

**阶段一交付物**: 项目框架、数据库脚本

### 2.2 阶段二：核心功能实现 (Day 2-3)

#### T2.1 实体层
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T2.1.1 | WorkItem实体类 | 0.5h |
| T2.1.2 | FieldDefinition实体类 | 0.5h |
| T2.1.3 | JacksonJsonTypeHandler | 1h |

#### T2.2 Mapper层
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T2.2.1 | WorkItemMapper | 0.5h |
| T2.2.2 | FieldDefinitionMapper | 0.25h |
| T2.2.3 | WorkItemMapper.xml动态查询 | 1h |

#### T2.3 Service层
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T2.3.1 | WorkItemService CRUD | 2h |
| T2.3.2 | WorkItemService动态查询 | 1.5h |
| T2.3.3 | FieldDefinitionService CRUD | 1.5h |

**阶段二交付物**: 完整的业务逻辑层

### 2.3 阶段三：API层开发 (Day 3-4)

#### T3.1 DTO层
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T3.1.1 | WorkItem请求/响应DTO | 1h |
| T3.1.2 | FieldDefinition请求/响应DTO | 1h |
| T3.1.3 | 通用分页响应DTO | 0.5h |
| T3.1.4 | 动态查询请求DTO | 0.5h |

#### T3.2 Controller层
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T3.2.1 | WorkItemController | 1.5h |
| T3.2.2 | FieldDefinitionController | 1h |
| T3.2.3 | Result统一响应类 | 0.5h |

**阶段三交付物**: RESTful API

### 2.4 阶段四：测试与验证 (Day 5)

#### T4.1 单元测试
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T4.1.1 | WorkItemService测试 | 1.5h |
| T4.1.2 | FieldDefinitionService测试 | 1h |
| T4.1.3 | Controller层测试 | 1.5h |

#### T4.2 文档更新
| 任务 | 描述 | 估计工时 |
|------|------|----------|
| T4.2.1 | 更新SDD文档 | 0.5h |
| T4.2.2 | 编写README | 0.5h |

**阶段四交付物**: 测试报告、文档

## 3. 任务依赖关系

```
T1.1 项目初始化
    ↓
T1.2 数据库设计 → T2.1 实体层
                        ↓
                   T2.2 Mapper层
                        ↓
                   T2.3 Service层
                        ↓
                   T3.1 DTO层 → T3.2 Controller层
                        ↓
                   T4.1 单元测试
                        ↓
                   T4.2 文档更新
```

## 4. 每日计划

### Day 1 - 基础框架
- [ ] T1.1 项目初始化
- [ ] T1.2 数据库设计
- [ ] T1.3 配置文件

### Day 2 - 实体与Mapper
- [ ] T2.1.1 WorkItem实体
- [ ] T2.1.2 FieldDefinition实体
- [ ] T2.1.3 JSON处理器
- [ ] T2.2 Mapper层

### Day 3 - Service层
- [ ] T2.3.1 WorkItemService CRUD
- [ ] T2.3.2 WorkItemService动态查询
- [ ] T2.3.3 FieldDefinitionService
- [ ] T3.1.1 DTO定义

### Day 4 - API层
- [ ] T3.1.2-4 剩余DTO
- [ ] T3.2.1 WorkItemController
- [ ] T3.2.2 FieldDefinitionController

### Day 5 - 测试与文档
- [ ] T4.1 单元测试
- [ ] T4.2 文档更新
- [ ] 集成验证

## 5. 资源需求

### 5.1 技术栈
| 类别 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.2 |
| ORM | MyBatis Plus 3.5 |
| 数据库 | PostgreSQL |
| 构建 | Maven |
| 测试 | JUnit 5, Mockito |

### 5.2 开发工具
- IDE: IntelliJ IDEA / VS Code
- 数据库客户端: pgAdmin / DBeaver
- API测试: Postman / curl

## 6. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| JSONB查询性能 | 中 | 创建GIN索引 |
| 动态SQL安全 | 高 | 参数化查询，禁止拼接 |
| 字段类型校验 | 中 | Service层统一校验 |

## 7. 质量标准

### 7.1 代码质量
- 遵循Google Java Style Guide
- 命名规范：类名UpperCamelCase，方法/变量lowerCamelCase
- 方法长度不超过50行
- 类文件不超过500行

### 7.2 测试覆盖
- Service层覆盖率 >= 80%
- 核心业务逻辑100%覆盖

### 7.3 评审要求
- 代码提交前完成自检
- 提交信息遵循Conventional Commits规范
