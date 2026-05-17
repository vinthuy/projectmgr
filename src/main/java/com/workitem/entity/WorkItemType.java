package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_item_type")
public class WorkItemType {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String typeKey;

    private String typeName;

    private String description;

    private String icon;

    private String typeCategory; // STANDARD, SUBTASK

    private Integer hierarchyLevel;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
