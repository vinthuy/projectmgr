package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class WorkItemResponse {
    private Long id;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String assignee;
    private Map<String, Object> customFields;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FieldDefinitionResponse> availableFields;
}
