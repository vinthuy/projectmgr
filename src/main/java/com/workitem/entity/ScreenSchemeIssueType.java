package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 屏幕方案与工作项类型的关联实体
 */
@Data
@TableName("screen_scheme_issue_type")
public class ScreenSchemeIssueType {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long schemeId;
    
    private Long issueTypeId;
    
    private Long createScreenId;
    
    private Long editScreenId;
    
    private Long viewScreenId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
