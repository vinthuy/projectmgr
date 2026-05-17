package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("issue_link")
public class IssueLink {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long linkTypeId;

    private Long sourceItemId; // 源工作项ID（主动方）

    private Long targetItemId; // 目标工作项ID（被动方）

    private String comment;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
