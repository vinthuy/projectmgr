package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 屏幕项实体（Screen与Field的关联）
 */
@Data
@TableName("screen_item")
public class ScreenItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long screenId;
    
    private Long screenTabId;
    
    private Long fieldDefinitionId;
    
    private Integer displayOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
