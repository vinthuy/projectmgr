package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作流方案实体
 */
@Data
@TableName("workflow_scheme")
public class WorkflowScheme {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String schemeKey;

    private String schemeName;

    private String description;

    private Boolean isSystem;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
