package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Screen创建请求
 */
@Data
public class ScreenCreateRequest {
    @NotBlank(message = "屏幕名称不能为空")
    private String screenName;
    
    private String description;
    
    private List<Long> fieldIds;  // 初始字段列表
}
