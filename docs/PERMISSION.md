# 权限方案设计文档

## 1. 概述

### 1.1 目的
设计实现类似Jira的权限控制系统，支持：
- 权限方案 (Permission Schemes)
- 项目角色 (Project Roles)
- 全局权限 (Global Permissions)
- 细粒度字段权限 (Field-level Permissions)

### 1.2 设计目标
```
Jira权限模型:
┌─────────────────────────────────────────────────────────────────┐
│                   Global Permissions             │
│   (Browse Sites, Administer Sites, etc.)    │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                 Permission Scheme             │
│   +----------------------------------+  │
│   | Browse Projects                   │  │
│   | Create Issues                    │  │
│   | Edit Issues                      │  │
│   | Delete Issues                   │  │
│   | Transition Issues               │  │
│   +----------------------------------+  │
└─────────────────────────────────────────────────────────────────┘
                          │
              ┌────────────┴────────────┐
              ▼                         ▼
┌─────────────────────┐      ┌─────────────────────┐
│   Project A        │      │    Project B       │
│   +-------------+  │      │   +-------------+  │
│   | Developers  │  │      │   | QA Team    │  │
│   | Testers    │  │      │   | Testers    │  │
│   | Viewers    │  │      │   | Viewers    │  │
│   +-------------+  │      │   +-------------+  │
└─────────────────────┘      └─────────────────────┘
```

---

## 2. 数据模型

### 2.1 全局权限表

```sql
-- =============================================
-- Permission Tables
-- =============================================

-- 全局权限定义
DROP TABLE IF EXISTS permission_definition CASCADE;
CREATE TABLE permission_definition (
    id BIGSERIAL PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    permission_name VARCHAR(200) NOT NULL,
    permission_name_zh VARCHAR(200),
    description TEXT,
    category VARCHAR(50),
    is_system BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 权限方案
DROP TABLE IF EXISTS permission_scheme CASCADE;
CREATE TABLE permission_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);

-- 权限授予
DROP TABLE IF EXISTS permission_grant CASCADE;
CREATE TABLE permission_grant (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    permission_key VARCHAR(100) NOT NULL,
    grant_type VARCHAR(50) NOT NULL,
    grant_key VARCHAR(100),
    UNIQUE(scheme_id, permission_key, grant_type, grant_key)
);

-- grant_type: GROUP | USER | PROJECT_ROLE
```

### 2.2 项目角色表

```sql
-- 项目角色
DROP TABLE IF EXISTS project_role CASCADE;
CREATE TABLE project_role (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    role_name VARCHAR(200) NOT NULL,
    role_name_zh VARCHAR(200),
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, role_key)
);

-- 项目角色成员
DROP TABLE IF EXISTS project_role_actor CASCADE;
CREATE TABLE project_role_actor (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    actor_type VARCHAR(50) NOT NULL,
    actor_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, role_id, actor_type, actor_key)
);

-- actor_type: USER | GROUP
```

### 2.3 字段权限表

```sql
-- 字段权限配置
DROP TABLE IF EXISTS field_permission CASCADE;
CREATE TABLE field_permission (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT,
    role_id BIGINT,
    field_definition_id BIGINT NOT NULL,
    can_view BOOLEAN DEFAULT TRUE,
    can_edit BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_id, role_id, field_definition_id)
);
```

---

## 3. 预置权限

### 3.1 全局权限定义

```sql
-- 插入全局权限
INSERT INTO permission_definition (permission_key, permission_name, permission_name_zh, description, category) VALUES
-- 浏览权限
('browse_projects', 'Browse Projects', '浏览项目', 'Browse projects', 'BROWSE'),
('browse_issues', 'Browse Issues', '浏览工作项', 'Browse issues', 'BROWSE'),

-- 创建权限
('create_issues', 'Create Issues', '创建工作项', 'Create issues', 'CREATE'),
('create_attachments', 'Create Attachments', '创建附件', 'Create attachments', 'CREATE'),

-- 编辑权限  
('edit_issues', 'Edit Issues', '编辑工作项', 'Edit issues', 'EDIT'),
('edit_own_issues', 'Edit Own Issues', '编辑自己工作项', 'Edit own issues', 'EDIT'),

-- 删除权限
('delete_issues', 'Delete Issues', '删除工作项', 'Delete issues', 'DELETE'),
('delete_own_issues', 'Delete Own Issues', '删除自己工作项', 'Delete own issues', 'DELETE'),

-- 转换权限
('transition_issues', 'Transition Issues', '转换工作项', 'Transition issues', 'TRANSITION'),

-- 分配权限
('assign_issues', 'Assign Issues', '分配工作项', 'Assign issues', 'ASSIGN'),
('assignable_issues', 'Assignable Issues', '可分配工作项', 'Assignable issues', 'ASSIGN'),

-- 评论权限
('comment_issues', 'Comment Issues', '评论工作项', 'Comment on issues', 'COMMENT'),
('comment_own_issues', 'Comment Own Issues', '评论自己工作项', 'Comment on own issues', 'COMMENT'),

-- 项目权限
('manage_project', 'Manage Project', '管理项目', 'Manage project settings', 'PROJECT'),
('manage_versions', 'Manage Versions', '管理版本', 'Manage versions', 'PROJECT'),
('manage_components', 'Manage Components', '管理组件', 'Manage components', 'PROJECT'),

-- 工作流权限
('manage_workflows', 'Manage Workflows', '管理工作流', 'Manage workflows', 'WORKFLOW'),

-- 系统权限
('admin_jira', 'Jira System Administration', '系统管理', 'Jira system administration', 'ADMIN'),
('admin_projects', 'Project Administration', '项目管理', 'Project administration', 'ADMIN');
```

### 3.2 预置项目角色

```sql
-- 插入预置项目角色
INSERT INTO project_role (tenant_id, role_key, role_name, role_name_zh, description, is_default) VALUES
(0, 'Developers', 'Developers', '开发人员', 'Can edit and manage issues', TRUE),
(0, 'Administrators', 'Project Administrators', '项目管理员', 'Can manage project settings', TRUE),
(0, 'Users', 'Users', '用户', 'Can create and view issues', TRUE),
(0, 'Viewers', 'Viewers', '查看者', 'Can view issues only', TRUE),
(0, 'Testers', 'Testers', '测试人员', 'Can test issues', TRUE);
```

### 3.3 默认权限方案

```sql
-- Users角色方案
INSERT INTO permission_scheme (tenant_id, scheme_key, scheme_name, description, is_default) VALUES
(0, 'default', 'Default Permission Scheme', 'Default scheme', TRUE);

-- 授予权限
INSERT INTO permission_grant (scheme_id, permission_key, grant_type, grant_key) VALUES
(1, 'browse_projects', 'GROUP', 'users'),
(1, 'browse_issues', 'GROUP', 'users'),
(1, 'create_issues', 'GROUP', 'users'),
(1, 'comment_issues', 'GROUP', 'users'),
(1, 'create_attachments', 'GROUP', 'users');

INSERT INTO permission_grant (scheme_id, permission_key, grant_type, grant_key) VALUES
(1, 'edit_issues', 'GROUP', 'developers'),
(1, 'delete_issues', 'GROUP', 'developers'),
(1, 'transition_issues', 'GROUP', 'developers'),
(1, 'assign_issues', 'GROUP', 'developers');

INSERT INTO permission_grant (scheme_id, permission_key, grant_type, grant_key) VALUES
(1, 'manage_project', 'GROUP', 'administrators'),
(1, 'manage_versions', 'GROUP', 'administrators'),
(1, 'manage_workflows', 'GROUP', 'administrators'),
(1, 'admin_projects', 'GROUP', 'administrators');
```

---

## 4. 权限检查服务

### 4.1 核心服务

```java
public class PermissionService {
    
    /**
     * 检查用户是否有权限
     */
    public boolean hasPermission(User user, String permissionKey) {
        return hasPermission(user, permissionKey, null, null);
    }
    
    /**
     * 检查用户是否有项目权限
     */
    public boolean hasPermission(User user, String permissionKey, Long projectId, Long workItemId) {
        // 1. 检查全局权限
        if (hasGlobalPermission(user, permissionKey)) {
            return true;
        }
        
        // 2. 获取用户所在项目角色
        List<String> projectRoles = getProjectRoles(user, projectId);
        
        // 3. 检查权限方案中的权限
        Long schemeId = getPermissionSchemeId(projectId);
        return checkSchemePermission(schemeId, permissionKey, projectRoles);
    }
    
    private boolean hasGlobalPermission(User user, String permissionKey) {
        // 管理员拥有所有权限
        if (user.isAdmin()) {
            return true;
        }
        
        // 检查全局权限表
        return globalPermissionMapper.exists(user.getId(), permissionKey);
    }
    
    private boolean checkSchemePermission(Long schemeId, String permissionKey, 
                                        List<String> roles) {
        List<PermissionGrant> grants = permissionGrantMapper
            .findBySchemeAndPermission(schemeId, permissionKey);
        
        for (PermissionGrant grant : grants) {
            switch (grant.getGrantType()) {
                case "GROUP":
                    // 检查用户组
                    if (userInGroups(grant.getGrantKey())) return true;
                    break;
                case "USER":
                    if (grant.getGrantKey().equals(currentUser.getUsername())) 
                        return true;
                    break;
                case "PROJECT_ROLE":
                    if (roles.contains(grant.getGrantKey())) return true;
                    break;
            }
        }
        return false;
    }
}
```

### 4.2 工作项级别权限

```java
public class WorkItemPermissionService {
    
    /**
     * 检查用户是否可以编辑工作项
     */
    public boolean canEdit(User user, WorkItem workItem) {
        // 1. 检查项目编辑权限
        if (!permissionService.hasPermission(user, "edit_issues", workItem.getProjectId(), workItem.getId())) {
            return false;
        }
        
        // 2. 检查是否是分配给自己的工作项（可编辑自己的）
        if (permissionService.hasPermission(user, "edit_own_issues", workItem.getProjectId())) {
            if (workItem.getAssigneeUserId().equals(user.getId())) {
                return true;
            }
        }
        
        // 3. 检查报告人权限
        if (workItem.getReporterUserId().equals(user.getId())) {
            // 可能有限制编辑自己的报告
        }
        
        // 4. 最终检查状态是否允许编辑
        if (workItem.getStatus().equals("Closed")) {
            // 已关闭的工作项可能不允许编辑
            return permissionService.hasPermission(user, "edit_closed_issues", 
                workItem.getProjectId());
        }
        
        return true;
    }
    
    /**
     * 检查用户是否可以删除工作项
     */
    public boolean canDelete(User user, WorkItem workItem) {
        if (!permissionService.hasPermission(user, "delete_issues", workItem.getProjectId())) {
            return false;
        }
        
        // 检查是否是创建者
        if (permissionService.hasPermission(user, "delete_own_issues", workItem.getProjectId())) {
            return workItem.getCreatedBy().equals(user.getId());
        }
        
        return true;
    }
}
```

### 4.3 字段权限检查

```java
public class FieldPermissionService {
    
    /**
     * 获取字段在特定上下文下的权限
     */
    public FieldPermission getFieldPermission(String fieldKey, User user, 
                                          Long projectId, Long issueTypeId) {
        // 1. 获取项目角色
        List<String> roles = getProjectRoles(user, projectId);
        
        // 2. 查询字段权限配置
        FieldPermission perm = fieldPermissionMapper.findByFieldAndRoles(
            fieldKey, projectId, roles);
        
        // 3. 如果没有特定配置，返回默认
        if (perm == null) {
            perm = getDefaultFieldPermission(fieldKey);
        }
        
        // 4. 检查是否有项目级别覆盖
        if (projectId != null) {
            FieldPermission override = fieldPermissionMapper.findGlobal(fieldKey);
            if (override != null) {
                perm = mergePermissions(perm, override);
            }
        }
        
        return perm;
    }
}
```

---

## 5. API设计

### 5.1 权限方案API

| 方法 | 路径 | 说明 |
|-----|------|------|
| GET | /api/permissions/definitions | 权限定义列表 |
| GET | /api/permission-schemes | 权限方案列表 |
| POST | /api/permission-schemes | 创建权限方案 |
| GET | /api/permission-schemes/{id} | 权限方案详情 |
| PUT | /api/permission-schemes/{id} | 更新权限方案 |
| POST | /api/permission-schemes/{id}/grants | 添加权限 |
| DELETE | /api/permission-schemes/{id}/grants/{gid} | 删除权限 |

### 5.2 项目角色API

| 方法 | 路径 |说明 |
|-----|------|------|
| GET | /api/projects/{id}/roles | 项目角色列表 |
| POST | /api/projects/{id}/roles | 添加角色成员 |
| DELETE | /api/projects/{id}/roles/{roleId}/actors/{actorId} | 删除角色成员 |

### 5.3 权限检查API

| 方法 | 路径 | 说明 |
|-----|------|------|
| GET | /api/permissions/check | 检查权限 |
| GET | /api/projects/{id}/permissions | 项目权限列表 |

### 5.4 请求示例

**添加权限:**
```json
POST /api/permission-schemes/1/grants
Request:
{
  "permissionKey": "edit_issues",
  "grantType": "PROJECT_ROLE",
  "grantKey": "Developers"
}
```

**添加角色成员:**
```json
POST /api/projects/1/roles
Request:
{
  "roleKey": "Developers",
  "actorType": "USER",
  "actorKey": "john.doe"
}
```

**权限检查:**
```json
GET /api/permissions/check?permissionKey=edit_issues&projectId=1

Response:
{
  "code": 200,
  "data": {
    "hasPermission": true
  }
}
```

---

## 6. 权限与工作流集成

### 6.1 工作流中的权限检查

```java
public class WorkflowPermissionChecker {
    
    /**
     * 检查用户是否可以执行转换
     */
    public boolean canTransition(User user, WorkItem workItem, Transition transition) {
        // 1. 检查工作流转换权限
        if (!permissionService.hasPermission(user, "transition_issues", 
                                            workItem.getProjectId(), workItem.getId())) {
            return false;
        }
        
        // 2. 检查转换权限限制
        String transitionPermission = transition.getPermission();
        if (transitionPermission != null) {
            return permissionService.hasPermission(user, transitionPermission,
                workItem.getProjectId(), workItem.getId());
        }
        
        // 3. 检查当前状态是否允许转换
        String fromStatus = transition.getFromStatus();
        if (!workItem.getStatus().equals(fromStatus)) {
            return false;
        }
        
        return true;
    }
}
```

### 6.2 字段编辑权限

```java
/**
 * 在工作项更新时检查字段权限
 */
public class FieldPermissionInterceptor {
    
    @Autowired
    private FieldPermissionService fieldPermissionService;
    
    public void checkFieldEditPermission(WorkItem workItem, Map<String, Object> fields, 
                                   User user) {
        for (String fieldKey : fields.keySet()) {
            FieldPermission perm = fieldPermissionService.getFieldPermission(
                fieldKey, user, workItem.getProjectId(), workItem.getIssueTypeId());
            
            if (!perm.canEdit()) {
                throw new PermissionDeniedException(
                    "No permission to edit field: " + fieldKey);
            }
        }
    }
}
```

---

## 7. 验收标准

### 7.1 权限方案验收
- [ ] 支持创建权限方案
- [ ] 支持添加权限授予
- [ ] 支持GROUP/USER/PROJECT_ROLE授予类型
- [ ] 正确继承全局权限

### 7.2 项目角色验收
- [ ] 支持预置项目角色
- [ ] 支持添加角色成员
- [ ] 权限正确按角色分配

### 7.3 权限检查验收
- [ ] hasPermission正确检查
- [ ] 工作项级别权限检查
- [ ] 字段级别权限检查

### 7.4 场景测试

**场景1: 只有项目成员可以编辑工作项**
```
Permission: edit_issues
Grant: PROJECT_ROLE:Developers
```

**场景2: 只有特定用户可以删除**
```
Permission: delete_issues
Grant: USER:admin
```

**场景3: 只有组内成员可以创建**
```
Permission: create_issues
Grant: GROUP:developers
```

**场景4: 字段编辑权限**
```
Field: resolution
Permission: can_edit = PROJECT_ROLE:Developers
```