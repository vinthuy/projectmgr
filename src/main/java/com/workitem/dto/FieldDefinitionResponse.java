package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FieldDefinitionResponse {
    private Long id;
    private Long tenantId;
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private String dataType;
    private String description;
    private Boolean required;
    private Object defaultValue;
    private Object options;
    private String searcherKey;
    private String rendererKey;
    private Boolean isSystem;
    private Boolean isGlobal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
