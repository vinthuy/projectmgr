package com.workitem.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IssueLinkResponse {
    private Long id;
    private String linkTypeKey;
    private String linkTypeName; // 显示名称：阻塞/被阻塞
    private String direction; // inward/outward
    private Long relatedItemId; // 关联的工作项ID
    private String relatedItemKey; // 关联的工作项编号
    private String relatedItemSummary; // 关联的工作项摘要
    private String comment;
    private Long createdBy;
    private LocalDateTime createdAt;
}
