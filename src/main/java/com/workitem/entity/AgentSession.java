package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_session")
public class AgentSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String userId;
    private Long tenantId;
    private String title;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean isActive;
}
