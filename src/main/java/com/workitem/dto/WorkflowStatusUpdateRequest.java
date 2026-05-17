package com.workitem.dto;

import lombok.Data;

@Data
public class WorkflowStatusUpdateRequest {
    private String statusName;

    private String category;

    private Integer displayOrder;

    private Boolean isActive;
}
