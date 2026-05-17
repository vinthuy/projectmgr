package com.workitem.dto;

import lombok.Data;

@Data
public class FieldDefinitionUpdateRequest {
    private String fieldName;

    private String description;

    private Boolean required;

    private Object defaultValue;

    private Object options;

    private String searcherKey;

    private String rendererKey;
}
