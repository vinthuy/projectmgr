package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Screen响应
 */
@Data
public class ScreenResponse {
    private Long id;
    private String screenName;
    private String description;
    private Boolean isSystem;
    private List<ScreenTabResponse> tabs;  // 包含的Tab
    private Map<String, String> issueTypeMappings;  // Issue Type映射: operationType -> screenName
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
