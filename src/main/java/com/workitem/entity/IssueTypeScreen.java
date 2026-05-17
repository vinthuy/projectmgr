package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题类型与屏幕的关联实体
 */
@Data
@TableName("issue_type_screen")
public class IssueTypeScreen {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private Long workItemTypeId;
    
    private Long screenId;
    
    private String operationType; // CREATE/EDIT/VIEW
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
