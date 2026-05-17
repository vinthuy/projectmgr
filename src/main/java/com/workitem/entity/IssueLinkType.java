package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("issue_link_type")
public class IssueLinkType {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String linkKey; // blocks, relates_to, duplicates, clones

    private String inwardName; // 内向名称（被动语态）：被阻塞、相关于

    private String outwardName; // 外向名称（主动语态）：阻塞、关联到

    private String description;

    private Boolean isSystem;

    private Integer displayOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
