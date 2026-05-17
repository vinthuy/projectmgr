package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@TableName("agent_message")
public class AgentMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String messageId;
    private String role; // user, assistant, system
    private String content;
    
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Map<String, Object> metadata;
    
    private Integer feedbackScore;
    private String feedbackComment;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
