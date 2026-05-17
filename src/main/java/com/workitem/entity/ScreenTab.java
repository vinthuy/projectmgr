package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 屏幕标签页实体
 */
@Data
@TableName("screen_tab")
public class ScreenTab {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long screenId;
    
    private String tabName;
    
    private Integer displayOrder;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Boolean deleted;
}
