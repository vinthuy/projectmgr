package com.workitem.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 屏幕方案详情响应DTO
 */
@Data
public class ScreenSchemeDetailResponse {
    private Long id;
    private String schemeKey;
    private String schemeName;
    private String description;
    private Boolean isSystem;
    private List<ScreenSchemeIssueTypeResponse> mappings;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
