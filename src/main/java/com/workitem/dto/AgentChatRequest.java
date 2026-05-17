package com.workitem.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class AgentChatRequest {
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;
    
    @NotBlank(message = "用户输入不能为空")
    private String userInput;
    
    private Map<String, Object> context;
}
