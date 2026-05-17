package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class WorkflowStatusCreateRequest {
    @NotBlank(message = "状态编码不能为空")
    @Pattern(regexp = "^[A-Z_]+$", message = "状态编码只能包含大写字母和下划线")
    private String statusCode;

    @NotBlank(message = "状态名称不能为空")
    private String statusName;

    @NotBlank(message = "状态分类不能为空")
    private String category; // TO_DO, IN_PROGRESS, DONE

    private Integer displayOrder = 0;

    private Boolean isActive = true;
}
