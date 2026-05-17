package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("workflow_status")
public class WorkflowStatus {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String statusCode;

    private String statusName;

    private String category; // TO_DO, IN_PROGRESS, DONE

    private Integer displayOrder;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
