package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class WorkItemCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private String status;

    private String priority;

    private String assignee;

    private Map<String, Object> customFields;
}
