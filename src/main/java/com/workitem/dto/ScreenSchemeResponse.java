package com.workitem.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 屏幕方案响应DTO
 */
@Data
public class ScreenSchemeResponse {
    private Long id;
    private String schemeKey;
    private String schemeName;
    private String description;
    private Boolean isSystem;
    private Integer issueTypeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
