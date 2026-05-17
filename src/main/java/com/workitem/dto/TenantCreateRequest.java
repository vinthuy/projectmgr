package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class TenantCreateRequest {
    @NotBlank(message = "租户标识不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]*$", message = "租户标识只能包含小写字母、数字、下划线和横线，且必须以字母开头")
    private String tenantKey;

    @NotBlank(message = "租户名称不能为空")
    private String tenantName;

    private String description;

    private String licenseType = "FREE";

    private Integer maxUsers = 10;

    private Integer maxProjects = 5;
}
