package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class IssueLinkTypeCreateRequest {
    @NotBlank(message = "关系标识不能为空")
    private String linkKey;

    @NotBlank(message = "内向名称不能为空")
    private String inwardName;

    @NotBlank(message = "外向名称不能为空")
    private String outwardName;

    private String description;

    private Integer displayOrder = 0;
}
