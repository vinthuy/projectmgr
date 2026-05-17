package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 工作流方案与工作项类型的关联实体
 */
@Data
@TableName("workflow_scheme_issue_type")
public class WorkflowSchemeIssueType {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long schemeId;

    private Long issueTypeId;

    private Long workflowId;

    private String initialStatus;
}
