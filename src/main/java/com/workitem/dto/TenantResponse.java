package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantResponse {
    private Long id;

    private String tenantKey;

    private String tenantName;

    private String description;

    private String status;

    private String licenseType;

    private Integer maxUsers;

    private Integer maxProjects;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
