package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 屏幕方案实体
 */
@Data
@TableName("screen_scheme")
public class ScreenScheme {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String schemeKey;
    
    private String schemeName;
    
    private String description;
    
    private Boolean isSystem;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
