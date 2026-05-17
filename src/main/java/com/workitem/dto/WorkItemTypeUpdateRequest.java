package com.workitem.dto;

import lombok.Data;

@Data
public class WorkItemTypeUpdateRequest {
    private String typeName;

    private String description;

    private String icon;

    private String typeCategory;

    private Integer hierarchyLevel;
}
