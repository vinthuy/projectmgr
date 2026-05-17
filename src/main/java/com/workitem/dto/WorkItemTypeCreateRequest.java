package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WorkItemTypeCreateRequest {
    @NotBlank(message = "类型标识不能为空")
    @Pattern(regexp = "^[a-z_]+$", message = "类型标识只能包含小写字母和下划线")
    private String typeKey;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    private String description;

    private String icon;

    private String typeCategory = "STANDARD"; // STANDARD, SUBTASK

    private Integer hierarchyLevel = 0;
}
