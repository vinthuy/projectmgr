package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName("work_item")
public class WorkItem {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String issueKey;

    private String workItemType;

    private String title;

    private String description;

    private String status;

    private String priority;

    private String assignee;

    private String reporter;

    @TableField(typeHandler = JacksonJsonTypeHandler.class)
    private Map<String, Object> customFields;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
