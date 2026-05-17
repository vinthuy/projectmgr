-- =============================================
-- Jira-Like Work Item Management System
-- Database Schema for PostgreSQL 17
-- =============================================

-- =============================================
-- Tenant Module
-- =============================================

DROP TABLE IF EXISTS tenant CASCADE;
CREATE TABLE tenant (
    id BIGSERIAL PRIMARY KEY,
    tenant_key VARCHAR(50) NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    license_type VARCHAR(50) DEFAULT 'FREE',
    max_users INTEGER DEFAULT 10,
    max_projects INTEGER DEFAULT 5,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE
);

CREATE INDEX idx_tenant_key ON tenant(tenant_key);

-- Users
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(200) NOT NULL,
    display_name VARCHAR(200),
    password_hash VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, username),
    UNIQUE(tenant_id, email));

CREATE INDEX idx_user_tenant ON sys_user(tenant_id);
CREATE INDEX idx_user_email ON sys_user(email);

-- Tenant Members
DROP TABLE IF EXISTS tenant_member CASCADE;
CREATE TABLE tenant_member (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, user_id));

-- =============================================
-- Project Module
-- =============================================

DROP TABLE IF EXISTS project CASCADE;
CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_key VARCHAR(10) NOT NULL,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,
    lead_user_id BIGINT,
    type_scheme_id BIGINT,
    workflow_scheme_id BIGINT,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_key));

CREATE INDEX idx_project_tenant ON project(tenant_id);
CREATE INDEX idx_project_key ON project(project_key);

-- Project Members
DROP TABLE IF EXISTS project_member CASCADE;
CREATE TABLE project_member (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role_key VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, user_id));

-- Project Template
DROP TABLE IF EXISTS project_template CASCADE;
CREATE TABLE project_template (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    template_key VARCHAR(50) NOT NULL,
    template_name VARCHAR(200) NOT NULL,
    description TEXT,
    type_scheme_id BIGINT,
    workflow_scheme_id BIGINT,
    default_field_values JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, template_key));

-- =============================================
-- Issue Type Module
-- =============================================

DROP TABLE IF EXISTS issue_type CASCADE;
CREATE TABLE issue_type (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    type_key VARCHAR(50) NOT NULL,
    type_name VARCHAR(200) NOT NULL,
    description TEXT,
    icon VARCHAR(100),
    type_category VARCHAR(50) DEFAULT 'STANDARD',
    hierarchy_level INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, type_key));

CREATE INDEX idx_issuetype_tenant ON issue_type(tenant_id);

-- Type Scheme
DROP TABLE IF EXISTS type_scheme CASCADE;
CREATE TABLE type_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key));

-- Type Scheme Issue Type Relation
DROP TABLE IF EXISTS type_scheme_issue_type CASCADE;
CREATE TABLE type_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    field_layout_id BIGINT,
    UNIQUE(scheme_id, issue_type_id));

-- =============================================
-- Field Module
-- =============================================

DROP TABLE IF EXISTS field_definition CASCADE;
CREATE TABLE field_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    field_name VARCHAR(200) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    description TEXT,
    required BOOLEAN DEFAULT FALSE,
    default_value JSONB,
    options JSONB,
    searcher_key VARCHAR(100),
    renderer_key VARCHAR(100),
    is_system BOOLEAN DEFAULT FALSE,
    is_global BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, field_key));

CREATE INDEX idx_field_tenant ON field_definition(tenant_id);

-- Field Layout
DROP TABLE IF EXISTS field_layout CASCADE;
CREATE TABLE field_layout (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT,
    field_definition_id BIGINT NOT NULL,
    display_name VARCHAR(200),
    description TEXT,
    field_sequence INTEGER DEFAULT 0,
    is_visible BOOLEAN DEFAULT TRUE,
    is_editable BOOLEAN DEFAULT TRUE,
    is_required BOOLEAN DEFAULT FALSE,
    rendering_config JSONB,
    UNIQUE(scheme_id, field_definition_id, issue_type_id));

-- =============================================
-- Workflow Module
-- =============================================

DROP TABLE IF EXISTS workflow_definition CASCADE;
CREATE TABLE workflow_definition (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    workflow_key VARCHAR(50) NOT NULL,
    workflow_name VARCHAR(200) NOT NULL,
    description TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    version INTEGER DEFAULT 1,
    status VARCHAR(50) DEFAULT 'DRAFT',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, workflow_key, version));

CREATE INDEX idx_workflow_tenant ON workflow_definition(tenant_id);

-- Workflow Step
DROP TABLE IF EXISTS workflow_step CASCADE;
CREATE TABLE workflow_step (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    step_key VARCHAR(50) NOT NULL,
    step_name VARCHAR(200) NOT NULL,
    step_sequence INTEGER DEFAULT 0,
    status_category VARCHAR(50),
    UNIQUE(workflow_id, step_key));

-- Workflow Transition
DROP TABLE IF EXISTS workflow_transition CASCADE;
CREATE TABLE workflow_transition (
    id BIGSERIAL PRIMARY KEY,
    workflow_id BIGINT NOT NULL,
    transition_key VARCHAR(50) NOT NULL,
    transition_name VARCHAR(200) NOT NULL,
    source_step_id BIGINT NOT NULL,
    target_step_id BIGINT NOT NULL,
    transition_type VARCHAR(50) DEFAULT 'DIRECT',
    conditions JSONB,
    validators JSONB,
    post_functions JSONB,
    UNIQUE(workflow_id, transition_key));

-- Workflow Scheme
DROP TABLE IF EXISTS workflow_scheme CASCADE;
CREATE TABLE workflow_scheme (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    scheme_key VARCHAR(50) NOT NULL,
    scheme_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, scheme_key));

-- Workflow Scheme Issue Type
DROP TABLE IF EXISTS workflow_scheme_issue_type CASCADE;
CREATE TABLE workflow_scheme_issue_type (
    id BIGSERIAL PRIMARY KEY,
    scheme_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    workflow_id BIGINT NOT NULL,
    UNIQUE(scheme_id, issue_type_id));

-- =============================================
-- Automation Module
-- =============================================

DROP TABLE IF EXISTS automation_rule CASCADE;
CREATE TABLE automation_rule (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    event_type VARCHAR(100) NOT NULL,
    project_id BIGINT,
    issue_type_id BIGINT,
    is_enabled BOOLEAN DEFAULT TRUE,
    execution_order INTEGER DEFAULT 0,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE);

CREATE INDEX idx_automation_tenant ON automation_rule(tenant_id);
CREATE INDEX idx_automation_project ON automation_rule(project_id);

-- Automation Conditions
DROP TABLE IF EXISTS automation_condition CASCADE;
CREATE TABLE automation_condition (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    condition_key VARCHAR(100) NOT NULL,
    condition_config JSONB NOT NULL,
    logical_operator VARCHAR(10) DEFAULT 'AND',
    condition_sequence INTEGER DEFAULT 0);

-- Automation Actions
DROP TABLE IF EXISTS automation_action CASCADE;
CREATE TABLE automation_action (
    id BIGSERIAL PRIMARY KEY,
    rule_id BIGINT NOT NULL,
    action_key VARCHAR(100) NOT NULL,
    action_config JSONB NOT NULL,
    action_sequence INTEGER DEFAULT 0);

-- =============================================
-- Work Item Module (Core)
-- =============================================

DROP TABLE IF EXISTS work_item CASCADE;
CREATE TABLE work_item (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    issue_type_id BIGINT NOT NULL,
    item_key VARCHAR(100) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    workflow_step_id BIGINT,
    priority VARCHAR(50),
    resolution VARCHAR(50),
    assignee_user_id BIGINT,
    reporter_user_id BIGINT,
    parent_item_id BIGINT,
    custom_fields JSONB DEFAULT '{}',
    labels JSONB DEFAULT '[]',
    attachments JSONB DEFAULT '[]',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_id, item_key));

CREATE INDEX idx_work_item_tenant ON work_item(tenant_id);
CREATE INDEX idx_work_item_project ON work_item(project_id);
CREATE INDEX idx_work_item_status ON work_item(status);
CREATE INDEX idx_work_item_assignee ON work_item(assignee_user_id);
CREATE INDEX idx_work_item_type ON work_item(issue_type_id);
CREATE INDEX idx_work_item_custom_fields ON work_item USING GIN(custom_fields);

-- Work Item History
DROP TABLE IF EXISTS work_item_history CASCADE;
CREATE TABLE work_item_history (
    id BIGSERIAL PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    field_key VARCHAR(100) NOT NULL,
    old_value JSONB,
    new_value JSONB,
    changed_by BIGINT NOT NULL,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);

CREATE INDEX idx_history_item ON work_item_history(work_item_id);

-- Work Item Comments
DROP TABLE IF EXISTS work_item_comment CASCADE;
CREATE TABLE work_item_comment (
    id BIGSERIAL PRIMARY KEY,
    work_item_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    author_user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE);

CREATE INDEX idx_comment_item ON work_item_comment(work_item_id);

-- =============================================
-- View / Filter Module
-- =============================================

DROP TABLE IF EXISTS filter_view CASCADE;
CREATE TABLE filter_view (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    owner_user_id BIGINT NOT NULL,
    is_shared BOOLEAN DEFAULT FALSE,
    query_config JSONB NOT NULL,
    column_config JSONB,
    sort_config JSONB,
    group_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE);

CREATE INDEX idx_filter_tenant ON filter_view(tenant_id);
CREATE INDEX idx_filter_owner ON filter_view(owner_user_id);

-- Board Config
DROP TABLE IF EXISTS board_config CASCADE;
CREATE TABLE board_config (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    board_name VARCHAR(200) NOT NULL,
    board_type VARCHAR(50) DEFAULT 'SPRINT',
    filter_view_id BIGINT,
    column_field_key VARCHAR(100),
    column_mapping JSONB,
    swimlane_config JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    UNIQUE(tenant_id, project_id, board_name));

-- =============================================
-- Initial Data: System Fields
-- =============================================

INSERT INTO field_definition (tenant_id, field_key, field_name, field_type, data_type, is_system, is_global) VALUES
(0, 'summary', '摘要', 'TEXT', 'text', TRUE, TRUE),
(0, 'description', '描述', 'RICHTEXT', 'html', TRUE, TRUE),
(0, 'status', '状态', 'SELECT', 'text', TRUE, TRUE),
(0, 'priority', '优先级', 'SELECT', 'text', TRUE, TRUE),
(0, 'assignee', '负责人', 'USER', 'user', TRUE, TRUE),
(0, 'reporter', '报告人', 'USER', 'user', TRUE, TRUE),
(0, 'labels', '标签', 'LABELS', 'array', TRUE, TRUE),
(0, 'components', '组件', 'MULTI_SELECT', 'array', TRUE, TRUE),
(0, 'fixVersions', '修复版本', 'MULTI_SELECT', 'array', TRUE, TRUE),
(0, 'dueDate', '截止日期', 'DATE', 'datetime', TRUE, TRUE),
(0, 'timeEstimate', '预估时间', 'NUMBER', 'number', TRUE, TRUE),
(0, 'timeSpent', '花费时间', 'NUMBER', 'number', TRUE, TRUE)
ON CONFLICT (tenant_id, field_key) DO NOTHING;

-- =============================================
-- Initial Data: Issue Types
-- =============================================

INSERT INTO issue_type (tenant_id, type_key, type_name, icon, type_category) VALUES
(0, 'task', '任务', 'task', 'STANDARD'),
(0, 'bug', '问题', 'bug', 'STANDARD'),
(0, 'story', '用户故事', 'bookmark', 'STANDARD'),
(0, 'epic', '史诗', 'star', 'STANDARD'),
(0, 'subtask', '子任务', 'sub-task', 'STANDARD')
ON CONFLICT (tenant_id, type_key) DO NOTHING;

-- =============================================
-- Initial Data: Workflow
-- =============================================

INSERT INTO workflow_definition (tenant_id, workflow_key, workflow_name, is_default, status) VALUES
(0, 'default', '默认工作流', TRUE, 'ACTIVE')
ON CONFLICT (tenant_id, workflow_key) DO NOTHING;

-- Insert workflow steps
INSERT INTO workflow_step (workflow_id, step_key, step_name, step_sequence, status_category)
SELECT w.id, 'TODO', '待处理', 1, 'OPEN'
FROM workflow_definition w WHERE w.workflow_key = 'default'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_step (workflow_id, step_key, step_name, step_sequence, status_category)
SELECT w.id, 'IN_PROGRESS', '进行中', 2, 'OPEN'
FROM workflow_definition w WHERE w.workflow_key = 'default'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_step (workflow_id, step_key, step_name, step_sequence, status_category)
SELECT w.id, 'DONE', '已完成', 3, 'DONE'
FROM workflow_definition w WHERE w.workflow_key = 'default'
ON CONFLICT DO NOTHING;

-- Insert transitions
INSERT INTO workflow_transition (workflow_id, transition_key, transition_name, source_step_id, target_step_id)
SELECT w.id, 'start', '开始', s1.id, s2.id
FROM workflow_definition w
JOIN workflow_step s1 ON w.id = s1.workflow_id AND s1.step_key = 'TODO'
JOIN workflow_step s2 ON w.id = s2.workflow_id AND s2.step_key = 'IN_PROGRESS'
ON CONFLICT DO NOTHING;

INSERT INTO workflow_transition (workflow_id, transition_key, transition_name, source_step_id, target_step_id)
SELECT w.id, 'complete', '完成', s1.id, s2.id
FROM workflow_definition w
JOIN workflow_step s1 ON w.id = s1.workflow_id AND s1.step_key = 'IN_PROGRESS'
JOIN workflow_step s2 ON w.id = s2.workflow_id AND s2.step_key = 'DONE'
ON CONFLICT DO NOTHING;

-- =============================================
-- Initial Data: Priority
-- =============================================

INSERT INTO field_definition (tenant_id, field_key, field_name, field_type, data_type, is_system, is_global, options) VALUES
(0, 'priority', '优先级', 'SELECT', 'text', TRUE, TRUE, '["highest", "high", "medium", "low", "lowest"]')
ON CONFLICT (tenant_id, field_key) DO NOTHING;

-- =============================================
-- Foreign Keys (After all tables created)
-- =============================================

-- ALTER TABLE project ADD CONSTRAINT fk_project_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id);
-- ALTER TABLE work_item ADD CONSTRAINT fk_workitem_project FOREIGN KEY (project_id) REFERENCES project(id);
-- Add more foreign keys as needed