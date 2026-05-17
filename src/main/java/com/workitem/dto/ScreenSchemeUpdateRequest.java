package com.workitem.dto;

import lombok.Data;

/**
 * 屏幕方案更新请求DTO
 */
@Data
public class ScreenSchemeUpdateRequest {
    private String schemeName;
    private String description;
}
