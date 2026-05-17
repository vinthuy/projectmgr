-- =============================================
-- Work Item Management System - Database Schema
-- =============================================

DROP TABLE IF EXISTS work_item CASCADE;
DROP TABLE IF EXISTS field_definition CASCADE;
DROP TABLE IF EXISTS work_item_type CASCADE;
DROP TABLE IF EXISTS workflow_status CASCADE;
DROP TABLE IF EXISTS issue_link CASCADE;
DROP TABLE IF EXISTS issue_link_type CASCADE;
DROP TABLE IF EXISTS issue_type_screen CASCADE;
DROP TABLE IF EXISTS screen_item CASCADE;
DROP TABLE IF EXISTS screen_tab CASCADE;
DROP TABLE IF EXISTS screen CASCADE;
DROP TABLE IF EXISTS tenant CASCADE;

-- =============================================
-- Table: tenant
-- =============================================
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_key VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    license_type VARCHAR(50) DEFAULT 'FREE',
    max_users INTEGER DEFAULT 10,
    max_projects INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- =============================================
-- Table: work_item_type (参考Jira issue_type设计)
-- =============================================
CREATE TABLE work_item_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    type_key VARCHAR(50) NOT NULL,
    type_name VARCHAR(200) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    type_category VARCHAR(50) DEFAULT 'STANDARD', -- STANDARD, SUBTASK
    hierarchy_level INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, type_key)
);

-- =============================================
-- Table: workflow_status
-- =============================================
CREATE TABLE workflow_status (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    status_code VARCHAR(50) NOT NULL,
    status_name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL, -- TO_DO, IN_PROGRESS, DONE
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, status_code)
);

-- =============================================
-- Table: work_item
-- =============================================
CREATE TABLE work_item (
    id BIGSERIAL PRIMARY KEY,
    issue_key VARCHAR(50) UNIQUE,
    work_item_type VARCHAR(50) NOT NULL DEFAULT 'TASK',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'OPEN',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    assignee VARCHAR(100),
    reporter VARCHAR(100),
    custom_fields JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

-- =============================================
-- Table: field_definition (参考Jira field_definition设计)
-- =============================================
CREATE TABLE field_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    field_key VARCHAR(100) NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(50) NOT NULL, -- TEXT, SELECT, MULTI_SELECT, NUMBER, DATE, USER, etc.
    data_type VARCHAR(50) NOT NULL DEFAULT 'text', -- text, number, datetime, user, array
    description TEXT,
    required BOOLEAN DEFAULT FALSE,
    default_value JSONB,
    options JSONB, -- 用于SELECT/MULTI_SELECT的选项
    searcher_key VARCHAR(100), -- 搜索器类型
    renderer_key VARCHAR(100), -- 渲染器类型
    is_system BOOLEAN DEFAULT FALSE, -- 是否系统字段
    is_global BOOLEAN DEFAULT FALSE, -- 是否全局字段
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, field_key)
);

-- =============================================
-- Table: issue_link_type (工作项关系类型)
-- =============================================
CREATE TABLE issue_link_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    link_key VARCHAR(50) NOT NULL,
    inward_name VARCHAR(100) NOT NULL,  -- 内向名称（被动语态）
    outward_name VARCHAR(100) NOT NULL, -- 外向名称（主动语态）
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, link_key)
);

-- =============================================
-- Table: issue_link (工作项关系实例)
-- =============================================
CREATE TABLE issue_link (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    link_type_id BIGINT NOT NULL REFERENCES issue_link_type(id),
    source_item_id BIGINT NOT NULL,  -- 源工作项ID（主动方）
    target_item_id BIGINT NOT NULL,  -- 目标工作项ID（被动方）
    comment TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    CHECK (source_item_id != target_item_id)  -- 不能自关联
);

CREATE INDEX idx_issue_link_source ON issue_link(source_item_id);
CREATE INDEX idx_issue_link_target ON issue_link(target_item_id);
CREATE INDEX idx_issue_link_type ON issue_link(link_type_id);
CREATE INDEX idx_issue_link_tenant ON issue_link(tenant_id);

-- =============================================
-- Table: screen (屏幕定义)
-- =============================================
CREATE TABLE screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    screen_name VARCHAR(100) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, screen_name)
);

CREATE INDEX idx_screen_tenant ON screen(tenant_id);

COMMENT ON TABLE screen IS '屏幕定义表';
COMMENT ON COLUMN screen.screen_name IS '屏幕名称';
COMMENT ON COLUMN screen.is_system IS '系统内置屏幕不可删除';

-- =============================================
-- Table: screen_tab (屏幕标签页)
-- =============================================
CREATE TABLE screen_tab (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    tab_name VARCHAR(100) NOT NULL,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_screentab_screen ON screen_tab(screen_id);

COMMENT ON TABLE screen_tab IS '屏幕标签页表';
COMMENT ON COLUMN screen_tab.tab_name IS '标签页名称，如：详情、人员、日期';

-- =============================================
-- Table: screen_item (屏幕项 - Screen与Field的关联)
-- =============================================
CREATE TABLE screen_item (
    id BIGSERIAL PRIMARY KEY,
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    screen_tab_id BIGINT REFERENCES screen_tab(id),
    field_definition_id BIGINT NOT NULL REFERENCES field_definition(id),
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(screen_id, field_definition_id)
);

CREATE INDEX idx_screenitem_screen ON screen_item(screen_id);
CREATE INDEX idx_screenitem_tab ON screen_item(screen_tab_id);
CREATE INDEX idx_screenitem_field ON screen_item(field_definition_id);

COMMENT ON TABLE screen_item IS '屏幕项表（Screen与Field的关联）';
COMMENT ON COLUMN screen_item.screen_tab_id IS '所属标签页，为空表示默认Tab';

-- =============================================
-- Table: issue_type_screen (问题类型与屏幕的关联)
-- =============================================
CREATE TABLE issue_type_screen (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    work_item_type_id BIGINT NOT NULL REFERENCES work_item_type(id),
    screen_id BIGINT NOT NULL REFERENCES screen(id),
    operation_type VARCHAR(20) NOT NULL, -- CREATE/EDIT/VIEW
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, work_item_type_id, operation_type)
);

CREATE INDEX idx_its_tenant ON issue_type_screen(tenant_id);
CREATE INDEX idx_its_type ON issue_type_screen(work_item_type_id);
CREATE INDEX idx_its_screen ON issue_type_screen(screen_id);

COMMENT ON TABLE issue_type_screen IS '问题类型与屏幕的关联表';
COMMENT ON COLUMN issue_type_screen.operation_type IS '操作类型：CREATE/EDIT/VIEW';

-- =============================================
-- Table: type_scheme (工作项类型方案)
-- =============================================
CREATE TABLE IF NOT EXISTS type_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);

CREATE INDEX IF NOT EXISTS idx_typescheme_tenant ON type_scheme(tenant_id);

COMMENT ON TABLE type_scheme IS '工作项类型方案表';
COMMENT ON COLUMN type_scheme.scheme_key IS '方案唯一标识';
COMMENT ON COLUMN type_scheme.is_system IS '系统内置方案不可删除';

-- =============================================
-- Table: type_scheme_issue_type (类型方案与工作项类型的关联)
-- =============================================
CREATE TABLE IF NOT EXISTS type_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL REFERENCES type_scheme(id),
    issue_type_id BIGINT NOT NULL REFERENCES work_item_type(id),
    display_order INTEGER DEFAULT 0,
    UNIQUE(scheme_id, issue_type_id)
);

CREATE INDEX IF NOT EXISTS idx_tsit_scheme ON type_scheme_issue_type(scheme_id);
CREATE INDEX IF NOT EXISTS idx_tsit_type ON type_scheme_issue_type(issue_type_id);

COMMENT ON TABLE type_scheme_issue_type IS '类型方案与工作项类型的关联表';
COMMENT ON COLUMN type_scheme_issue_type.display_order IS '显示顺序';

-- =============================================
-- Table: workflow_scheme (工作流方案)
-- =============================================
CREATE TABLE IF NOT EXISTS workflow_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);

CREATE INDEX IF NOT EXISTS idx_workflowscheme_tenant ON workflow_scheme(tenant_id);

COMMENT ON TABLE workflow_scheme IS '工作流方案表';
COMMENT ON COLUMN workflow_scheme.scheme_key IS '方案唯一标识';
COMMENT ON COLUMN workflow_scheme.is_system IS '系统内置方案不可删除';

-- =============================================
-- Table: workflow_scheme_issue_type (工作流方案与工作项类型的关联)
-- =============================================
CREATE TABLE IF NOT EXISTS workflow_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL REFERENCES workflow_scheme(id),
    issue_type_id BIGINT NOT NULL REFERENCES work_item_type(id),
    workflow_id BIGINT,
    initial_status VARCHAR(50),
    UNIQUE(scheme_id, issue_type_id)
);

CREATE INDEX IF NOT EXISTS idx_wsit_scheme ON workflow_scheme_issue_type(scheme_id);
CREATE INDEX IF NOT EXISTS idx_wsit_type ON workflow_scheme_issue_type(issue_type_id);

COMMENT ON TABLE workflow_scheme_issue_type IS '工作流方案与工作项类型的关联表';
COMMENT ON COLUMN workflow_scheme_issue_type.workflow_id IS '关联的工作流ID（暂未实现）';
COMMENT ON COLUMN workflow_scheme_issue_type.initial_status IS '初始状态码';

-- =============================================
-- Table: screen_scheme (屏幕方案)
-- =============================================
CREATE TABLE IF NOT EXISTS screen_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_system BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key)
);

CREATE INDEX IF NOT EXISTS idx_screenscheme_tenant ON screen_scheme(tenant_id);

COMMENT ON TABLE screen_scheme IS '屏幕方案表';
COMMENT ON COLUMN screen_scheme.scheme_key IS '方案唯一标识';
COMMENT ON COLUMN screen_scheme.is_system IS '系统内置方案不可删除';

-- =============================================
-- Table: screen_scheme_issue_type (屏幕方案与工作项类型的关联)
-- =============================================
CREATE TABLE IF NOT EXISTS screen_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL REFERENCES screen_scheme(id),
    issue_type_id BIGINT NOT NULL REFERENCES work_item_type(id),
    create_screen_id BIGINT REFERENCES screen(id),
    edit_screen_id BIGINT REFERENCES screen(id),
    view_screen_id BIGINT REFERENCES screen(id),
    UNIQUE(scheme_id, issue_type_id)
);

CREATE INDEX IF NOT EXISTS idx_ssit_scheme ON screen_scheme_issue_type(scheme_id);
CREATE INDEX IF NOT EXISTS idx_ssit_type ON screen_scheme_issue_type(issue_type_id);
CREATE INDEX IF NOT EXISTS idx_ssit_create_screen ON screen_scheme_issue_type(create_screen_id);
CREATE INDEX IF NOT EXISTS idx_ssit_view_screen ON screen_scheme_issue_type(view_screen_id);

COMMENT ON TABLE screen_scheme_issue_type IS '屏幕方案与工作项类型的关联表';
COMMENT ON COLUMN screen_scheme_issue_type.create_screen_id IS '创建和编辑时使用的屏幕';
COMMENT ON COLUMN screen_scheme_issue_type.view_screen_id IS '查看详情时使用的屏幕';

-- =============================================
-- Table: project_template (项目模板)
-- =============================================
CREATE TABLE IF NOT EXISTS project_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    template_key VARCHAR(50) NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    description TEXT,
    type_scheme_id BIGINT,
    workflow_scheme_id BIGINT,
    screen_scheme_id BIGINT REFERENCES screen_scheme(id),
    default_field_values JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, template_key)
);

CREATE INDEX IF NOT EXISTS idx_template_tenant ON project_template(tenant_id);
CREATE INDEX IF NOT EXISTS idx_template_key ON project_template(template_key);

COMMENT ON TABLE project_template IS '项目模板表';
COMMENT ON COLUMN project_template.screen_scheme_id IS '关联的屏幕方案ID';

-- =============================================
-- Indexes
-- =============================================
CREATE INDEX idx_tenant_key ON tenant(tenant_key);
CREATE INDEX idx_work_item_type_tenant ON work_item_type(tenant_id);
CREATE INDEX idx_workflow_status_tenant ON workflow_status(tenant_id);
CREATE INDEX idx_field_definition_tenant ON field_definition(tenant_id);
CREATE INDEX idx_field_definition_key ON field_definition(field_key);
CREATE INDEX idx_issue_link_type_tenant ON issue_link_type(tenant_id);
CREATE INDEX idx_work_item_type ON work_item(work_item_type);
CREATE INDEX idx_work_item_status ON work_item(status);
CREATE INDEX idx_work_item_priority ON work_item(priority);
CREATE INDEX idx_work_item_assignee ON work_item(assignee);
CREATE INDEX idx_work_item_reporter ON work_item(reporter);
CREATE INDEX idx_work_item_created_at ON work_item(created_at DESC);
CREATE INDEX idx_work_item_custom_fields ON work_item USING GIN (custom_fields);

-- =============================================
-- Initial Data: Default Tenant
-- =============================================
INSERT INTO tenant (tenant_key, tenant_name, description, status, license_type, max_users, max_projects) VALUES
('default', '默认租户', '系统默认租户', 'ACTIVE', 'FREE', 10, 5)
ON CONFLICT (tenant_key) DO NOTHING;

-- =============================================
-- Initial Data: Work Item Types (参考Jira标准类型)
-- =============================================
INSERT INTO work_item_type (tenant_id, type_key, type_name, description, icon, type_category, hierarchy_level) VALUES
(1, 'epic', '史诗', '大型功能集合，通常跨越多个迭代', 'Star', 'STANDARD', 0),
(1, 'story', '用户故事', '从用户角度描述的功能需求', 'Bookmark', 'STANDARD', 1),
(1, 'task', '任务', '普通工作任务', 'Document', 'STANDARD', 1),
(1, 'bug', '问题', '软件缺陷或错误', 'BugReport', 'STANDARD', 1),
(1, 'subtask', '子任务', '任务的细分项', 'Tickets', 'SUBTASK', 2)
ON CONFLICT (tenant_id, type_key) DO NOTHING;

-- =============================================
-- Initial Data: Workflow Statuses
-- =============================================
INSERT INTO workflow_status (tenant_id, status_code, status_name, category, display_order) VALUES
(1, 'OPEN', '待处理', 'TO_DO', 0),
(1, 'IN_PROGRESS', '进行中', 'IN_PROGRESS', 1),
(1, 'CODE_REVIEW', '代码审查', 'IN_PROGRESS', 2),
(1, 'TESTING', '测试中', 'IN_PROGRESS', 3),
(1, 'RESOLVED', '已解决', 'DONE', 4),
(1, 'CLOSED', '已关闭', 'DONE', 5),
(1, 'REOPENED', '重新打开', 'TO_DO', 6)
ON CONFLICT (tenant_id, status_code) DO NOTHING;

-- =============================================
-- Initial Data: Default Field Definitions (参考Jira系统字段)
-- =============================================
INSERT INTO field_definition (tenant_id, field_key, field_name, field_type, data_type, is_system, is_global) VALUES
(1, 'summary', '摘要', 'TEXT', 'text', TRUE, TRUE),
(1, 'description', '描述', 'RICHTEXT', 'html', TRUE, TRUE),
(1, 'status', '状态', 'SELECT', 'text', TRUE, TRUE),
(1, 'priority', '优先级', 'SELECT', 'text', TRUE, TRUE),
(1, 'assignee', '负责人', 'USER', 'user', TRUE, TRUE),
(1, 'reporter', '报告人', 'USER', 'user', TRUE, TRUE),
(1, 'labels', '标签', 'LABELS', 'array', TRUE, TRUE),
(1, 'components', '组件', 'MULTI_SELECT', 'array', TRUE, TRUE),
(1, 'fixVersions', '修复版本', 'MULTI_SELECT', 'array', TRUE, TRUE),
(1, 'dueDate', '截止日期', 'DATE', 'datetime', TRUE, TRUE),
(1, 'timeEstimate', '预估时间', 'NUMBER', 'number', TRUE, TRUE),
(1, 'timeSpent', '花费时间', 'NUMBER', 'number', TRUE, TRUE)
ON CONFLICT (tenant_id, field_key) DO NOTHING;

-- 自定义字段示例
INSERT INTO field_definition (tenant_id, field_key, field_name, field_type, data_type, description, required, options) VALUES
(1, 'severity', '严重程度', 'SELECT', 'text', '问题的严重程度', false, '["LOW", "MEDIUM", "HIGH", "CRITICAL"]'),
(1, 'component', '组件', 'TEXT', 'text', '所属组件模块', false, NULL),
(1, 'version', '版本', 'TEXT', 'text', '相关版本号', false, NULL),
(1, 'tags', '标签', 'MULTI_SELECT', 'array', '自定义标签', false, '["bug", "feature", "enhancement", "documentation"]')
ON CONFLICT (tenant_id, field_key) DO NOTHING;

-- =============================================
-- Initial Data: Issue Link Types (参考Jira标准关系类型)
-- =============================================
INSERT INTO issue_link_type (tenant_id, link_key, inward_name, outward_name, description, is_system, display_order) VALUES
(1, 'blocks', '被阻塞', '阻塞', '阻止另一个工作项的进行', TRUE, 1),
(1, 'relates_to', '相关于', '关联到', '与另一个工作项相关', TRUE, 2),
(1, 'duplicates', '被重复', '重复', '是另一个工作项的重复', TRUE, 3),
(1, 'clones', '被克隆', '克隆', '克隆自另一个工作项', TRUE, 4)
ON CONFLICT (tenant_id, link_key) DO NOTHING;

-- =============================================
-- Initial Data: Sample Work Items
-- =============================================
INSERT INTO work_item (issue_key, work_item_type, title, description, status, priority, assignee, reporter, custom_fields) VALUES
('PROJ-00001', 'TASK', 'Setup development environment', 'Install required tools and configure IDE', 'CLOSED', 'HIGH', 'john.doe', 'admin', '{"severity": "LOW", "component": "devops", "version": "1.0.0", "tags": ["feature"]}'),
('PROJ-00002', 'STORY', 'Implement user authentication', 'Add login and registration functionality', 'IN_PROGRESS', 'HIGH', 'jane.smith', 'admin', '{"severity": "HIGH", "component": "security", "version": "1.0.0", "tags": ["feature"]}'),
('PROJ-00003', 'BUG', 'Fix pagination bug', 'Page numbers not updating correctly', 'OPEN', 'MEDIUM', 'bob.wilson', 'admin', '{"severity": "MEDIUM", "component": "ui", "version": "0.9.5", "tags": ["bug"]}')
ON CONFLICT (issue_key) DO NOTHING;

-- =============================================
-- Initial Data: Default Screen
-- =============================================
INSERT INTO screen (tenant_id, screen_name, description, is_system) VALUES
(1, 'Default Screen', '默认屏幕', TRUE),
(1, 'Bug Screen', 'Bug跟踪屏幕，包含严重程度等字段', TRUE),
(1, 'Story Screen', '用户故事屏幕，简化字段', TRUE),
(1, 'Task Screen', '任务管理屏幕', TRUE)
ON CONFLICT (tenant_id, screen_name) DO NOTHING;

-- 为Default Screen创建默认Tab
INSERT INTO screen_tab (screen_id, tab_name, display_order)
SELECT id, '详情', 0 FROM screen WHERE screen_name = 'Default Screen' AND tenant_id = 1
ON CONFLICT DO NOTHING;

-- 为Bug Screen创建Tabs
INSERT INTO screen_tab (screen_id, tab_name, display_order)
SELECT id, tab.tab_name, tab.display_order
FROM screen s
CROSS JOIN (
    VALUES 
        ('详情', 0),
        ('人员', 1),
        ('日期', 2)
) AS tab(tab_name, display_order)
WHERE s.screen_name = 'Bug Screen' AND s.tenant_id = 1
ON CONFLICT DO NOTHING;

-- 为Story Screen创建Tabs
INSERT INTO screen_tab (screen_id, tab_name, display_order)
SELECT id, tab.tab_name, tab.display_order
FROM screen s
CROSS JOIN (
    VALUES 
        ('详情', 0),
        ('验收标准', 1)
) AS tab(tab_name, display_order)
WHERE s.screen_name = 'Story Screen' AND s.tenant_id = 1
ON CONFLICT DO NOTHING;

-- 为Task Screen创建Tabs
INSERT INTO screen_tab (screen_id, tab_name, display_order)
SELECT id, tab.tab_name, tab.display_order
FROM screen s
CROSS JOIN (
    VALUES 
        ('详情', 0),
        ('进度', 1)
) AS tab(tab_name, display_order)
WHERE s.screen_name = 'Task Screen' AND s.tenant_id = 1
ON CONFLICT DO NOTHING;

-- 将所有系统字段添加到默认Screen
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Default Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND fd.is_system = TRUE
  AND fd.tenant_id = 1
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 为Bug Screen添加字段（详情Tab）
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Bug Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND st.tab_name = '详情'
  AND fd.field_key IN ('summary', 'description', 'status', 'priority', 'severity', 'component', 'labels')
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 为Bug Screen添加人员字段（人员Tab）
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Bug Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND st.tab_name = '人员'
  AND fd.field_key IN ('assignee', 'reporter')
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 为Bug Screen添加日期字段（日期Tab）
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Bug Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND st.tab_name = '日期'
  AND fd.field_key IN ('dueDate')
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 为Story Screen添加字段（详情Tab）
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Story Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND st.tab_name = '详情'
  AND fd.field_key IN ('summary', 'description', 'status', 'priority', 'assignee', 'labels')
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 为Task Screen添加字段（详情Tab）
INSERT INTO screen_item (screen_id, screen_tab_id, field_definition_id, display_order)
SELECT 
    s.id,
    st.id,
    fd.id,
    ROW_NUMBER() OVER (ORDER BY fd.id) - 1
FROM screen s
CROSS JOIN screen_tab st
CROSS JOIN field_definition fd
WHERE s.screen_name = 'Task Screen' 
  AND s.tenant_id = 1
  AND st.screen_id = s.id
  AND st.tab_name = '详情'
  AND fd.field_key IN ('summary', 'description', 'status', 'priority', 'assignee', 'dueDate')
ON CONFLICT (screen_id, field_definition_id) DO NOTHING;

-- 关联所有Issue Type到默认Screen
INSERT INTO issue_type_screen (tenant_id, work_item_type_id, screen_id, operation_type)
SELECT 
    1,
    wit.id,
    s.id,
    op.operation
FROM work_item_type wit
CROSS JOIN screen s
CROSS JOIN (VALUES ('CREATE'), ('EDIT'), ('VIEW')) AS op(operation)
WHERE wit.tenant_id = 1
  AND s.screen_name = 'Default Screen'
  AND s.tenant_id = 1
ON CONFLICT (tenant_id, work_item_type_id, operation_type) DO NOTHING;

-- =============================================
-- Initial Data: Default Screen Schemes
-- =============================================
INSERT INTO screen_scheme (tenant_id, scheme_key, scheme_name, description, is_system) VALUES
(1, 'default-screen-scheme', '默认屏幕方案', '适用于所有工作项类型的默认屏幕配置', TRUE),
(1, 'software-dev-screen-scheme', '软件开发屏幕方案', '适用于软件研发项目的屏幕配置', TRUE)
ON CONFLICT (tenant_id, scheme_key) DO NOTHING;

-- 为默认屏幕方案配置映射
INSERT INTO screen_scheme_issue_type (scheme_id, issue_type_id, create_screen_id, edit_screen_id, view_screen_id)
SELECT 
    ss.id as scheme_id,
    wit.id as issue_type_id,
    s_default.id as create_screen_id,
    s_default.id as edit_screen_id,
    s_default.id as view_screen_id
FROM screen_scheme ss
CROSS JOIN work_item_type wit
CROSS JOIN screen s_default
WHERE ss.scheme_key = 'default-screen-scheme'
  AND ss.tenant_id = 1
  AND wit.tenant_id = 1
  AND s_default.screen_name = 'Default Screen'
  AND s_default.tenant_id = 1
ON CONFLICT (scheme_id, issue_type_id) DO NOTHING;

-- 为软件开发屏幕方案配置映射
INSERT INTO screen_scheme_issue_type (scheme_id, issue_type_id, create_screen_id, edit_screen_id, view_screen_id)
SELECT 
    ss.id as scheme_id,
    wit.id as issue_type_id,
    CASE wit.type_key
        WHEN 'epic' THEN s_epic.id
        WHEN 'story' THEN s_story.id
        WHEN 'task' THEN s_task.id
        WHEN 'bug' THEN s_bug.id
        ELSE s_default.id
    END as create_screen_id,
    CASE wit.type_key
        WHEN 'epic' THEN s_epic.id
        WHEN 'story' THEN s_story.id
        WHEN 'task' THEN s_task.id
        WHEN 'bug' THEN s_bug.id
        ELSE s_default.id
    END as edit_screen_id,
    CASE wit.type_key
        WHEN 'epic' THEN s_epic.id
        WHEN 'story' THEN s_story.id
        WHEN 'task' THEN s_task.id
        WHEN 'bug' THEN s_bug.id
        ELSE s_default.id
    END as view_screen_id
FROM screen_scheme ss
CROSS JOIN work_item_type wit
LEFT JOIN screen s_epic ON s_epic.screen_name = 'Epic Screen' AND s_epic.tenant_id = 1
LEFT JOIN screen s_story ON s_story.screen_name = 'Story Screen' AND s_story.tenant_id = 1
LEFT JOIN screen s_task ON s_task.screen_name = 'Task Screen' AND s_task.tenant_id = 1
LEFT JOIN screen s_bug ON s_bug.screen_name = 'Bug Screen' AND s_bug.tenant_id = 1
LEFT JOIN screen s_default ON s_default.screen_name = 'Default Screen' AND s_default.tenant_id = 1
WHERE ss.scheme_key = 'software-dev-screen-scheme'
  AND ss.tenant_id = 1
  AND wit.tenant_id = 1
ON CONFLICT (scheme_id, issue_type_id) DO NOTHING;

-- =============================================
-- Initial Data: Default Type Schemes
-- =============================================
INSERT INTO type_scheme (tenant_id, scheme_key, scheme_name, description, is_system) VALUES
(1, 'default-type-scheme', '默认类型方案', '包含所有标准工作项类型', TRUE),
(1, 'software-dev-type-scheme', '软件开发类型方案', '适用于软件研发项目的类型配置', TRUE)
ON CONFLICT (tenant_id, scheme_key) DO NOTHING;

-- 为默认类型方案配置所有类型
INSERT INTO type_scheme_issue_type (scheme_id, issue_type_id, display_order)
SELECT 
    ts.id as scheme_id,
    wit.id as issue_type_id,
    wit.hierarchy_level as display_order
FROM type_scheme ts
CROSS JOIN work_item_type wit
WHERE ts.scheme_key = 'default-type-scheme'
  AND ts.tenant_id = 1
  AND wit.tenant_id = 1
ON CONFLICT (scheme_id, issue_type_id) DO NOTHING;

-- =============================================
-- Initial Data: Default Workflow Schemes
-- =============================================
INSERT INTO workflow_scheme (tenant_id, scheme_key, scheme_name, description, is_system) VALUES
(1, 'default-workflow-scheme', '默认工作流方案', '标准工作流配置', TRUE),
(1, 'software-dev-workflow-scheme', '软件开发工作流方案', '适用于软件研发项目的工作流', TRUE)
ON CONFLICT (tenant_id, scheme_key) DO NOTHING;

-- 为默认工作流方案配置映射
INSERT INTO workflow_scheme_issue_type (scheme_id, issue_type_id, initial_status)
SELECT 
    ws.id as scheme_id,
    wit.id as issue_type_id,
    'OPEN' as initial_status
FROM workflow_scheme ws
CROSS JOIN work_item_type wit
WHERE ws.scheme_key = 'default-workflow-scheme'
  AND ws.tenant_id = 1
  AND wit.tenant_id = 1
ON CONFLICT (scheme_id, issue_type_id) DO NOTHING;

-- =============================================
-- Agent Module Tables
-- =============================================

DROP TABLE IF EXISTS agent_learning_log CASCADE;
DROP TABLE IF EXISTS agent_message CASCADE;
DROP TABLE IF EXISTS agent_session CASCADE;

-- =============================================
-- Table: agent_session
-- =============================================
CREATE TABLE agent_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    tenant_id BIGINT NOT NULL REFERENCES tenant(id),
    title VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_agent_session_user ON agent_session(user_id, tenant_id);
CREATE INDEX idx_agent_session_created ON agent_session(created_at DESC);

COMMENT ON TABLE agent_session IS 'Agent对话会话表';

-- =============================================
-- Table: agent_message
-- =============================================
CREATE TABLE agent_message (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES agent_session(session_id) ON DELETE CASCADE,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    role VARCHAR(20) NOT NULL, -- 'user' | 'assistant' | 'system'
    content TEXT NOT NULL,
    metadata JSONB, -- 存储额外信息: {intent, tools_used, confidence}
    feedback_score INTEGER, -- 用户评分 1-5
    feedback_comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_message_session ON agent_message(session_id, created_at);
CREATE INDEX idx_agent_message_role ON agent_message(role);

COMMENT ON TABLE agent_message IS 'Agent对话消息表';

-- =============================================
-- Table: agent_learning_log
-- =============================================
CREATE TABLE agent_learning_log (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL REFERENCES agent_session(session_id) ON DELETE CASCADE,
    user_input TEXT,
    detected_intent VARCHAR(50),
    action_taken VARCHAR(100),
    result_success BOOLEAN,
    optimization_applied TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agent_learning_intent ON agent_learning_log(detected_intent);
CREATE INDEX idx_agent_learning_success ON agent_learning_log(result_success);

COMMENT ON TABLE agent_learning_log IS 'Agent学习记录表';
