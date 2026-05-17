package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Issue Type Screen映射请求
 */
@Data
public class IssueTypeScreenMappingRequest {
    @NotNull(message = "问题类型ID不能为空")
    private Long workItemTypeId;
    
    @NotBlank(message = "操作类型不能为空")
    private String operationType;  // CREATE/EDIT/VIEW
    
    @NotNull(message = "屏幕ID不能为空")
    private Long screenId;
}
