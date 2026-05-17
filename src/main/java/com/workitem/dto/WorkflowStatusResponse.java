package com.workitem.dto;

import lombok.Data;

@Data
public class WorkflowStatusResponse {
    private Long id;
    private Long tenantId;
    private String statusCode;
    private String statusName;
    private String category;
    private Integer displayOrder;
    private Boolean isActive;
}
