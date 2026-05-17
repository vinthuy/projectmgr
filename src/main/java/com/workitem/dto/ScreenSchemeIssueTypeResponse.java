package com.workitem.dto;

import lombok.Data;

/**
 * 屏幕方案与工作项类型关联响应DTO
 */
@Data
public class ScreenSchemeIssueTypeResponse {
    private Long id;
    private Long schemeId;
    private Long issueTypeId;
    private String issueTypeKey;
    private String issueTypeName;
    private String issueTypeIcon;
    
    private Long createScreenId;
    private String createScreenName;
    
    private Long editScreenId;
    private String editScreenName;
    
    private Long viewScreenId;
    private String viewScreenName;
}
