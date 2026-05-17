package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("agent_learning_log")
public class AgentLearningLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String sessionId;
    private String userInput;
    private String detectedIntent;
    private String actionTaken;
    private Boolean resultSuccess;
    private String optimizationApplied;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
