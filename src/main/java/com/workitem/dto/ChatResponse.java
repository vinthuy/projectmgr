package com.workitem.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String sessionId;
    private String messageId;
    private String role;
    private String content;
    private List<Map<String, Object>> workItems;
    private String intent;
    private Double confidence;
    private List<String> suggestions;
    private Map<String, Object> metadata;
}
