package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueLinkCreateRequest {
    @NotBlank(message = "关系类型不能为空")
    private String linkTypeKey; // blocks, relates_to

    @NotNull(message = "源工作项ID不能为空")
    private Long sourceItemId;

    @NotNull(message = "目标工作项ID不能为空")
    private Long targetItemId;

    private String comment;
}
