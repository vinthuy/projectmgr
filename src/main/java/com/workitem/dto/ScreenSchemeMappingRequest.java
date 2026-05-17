package com.workitem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 屏幕方案映射请求DTO
 */
@Data
public class ScreenSchemeMappingRequest {
    @NotNull(message = "工作项类型ID不能为空")
    private Long issueTypeId;
    
    private Long createScreenId;
    
    private Long editScreenId;
    
    private Long viewScreenId;
}
