package com.workitem.dto;

import lombok.Data;

@Data
public class WorkItemTypeResponse {
    private Long id;
    private Long tenantId;
    private String typeKey;
    private String typeName;
    private String description;
    private String icon;
    private String typeCategory;
    private Integer hierarchyLevel;
}
