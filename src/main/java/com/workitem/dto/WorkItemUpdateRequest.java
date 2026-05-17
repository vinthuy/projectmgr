package com.workitem.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WorkItemUpdateRequest {
    private String title;
    private String description;
    private String status;
    private String priority;
    private String assignee;
    private Map<String, Object> customFields;
}
