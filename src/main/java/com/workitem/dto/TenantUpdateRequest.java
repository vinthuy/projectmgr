package com.workitem.dto;

import lombok.Data;

@Data
public class TenantUpdateRequest {
    private String tenantName;

    private String description;

    private String status;

    private String licenseType;

    private Integer maxUsers;

    private Integer maxProjects;
}
