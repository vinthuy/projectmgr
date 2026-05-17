package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 屏幕方案创建请求DTO
 */
@Data
public class ScreenSchemeCreateRequest {
    @NotBlank(message = "方案Key不能为空")
    private String schemeKey;
    
    @NotBlank(message = "方案名称不能为空")
    private String schemeName;
    
    private String description;
}
