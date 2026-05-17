package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Issue Type Screen映射响应
 */
@Data
public class IssueTypeScreenResponse {
    private Long id;
    private Long tenantId;
    private Long workItemTypeId;
    private String workItemTypeName;
    private Long screenId;
    private String screenName;
    private String operationType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
