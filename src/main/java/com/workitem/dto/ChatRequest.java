package com.workitem.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class ChatRequest {
    private String sessionId;
    
    @NotBlank(message = "消息内容不能为空")
    private String message;
    
    private Map<String, Object> metadata;
}
