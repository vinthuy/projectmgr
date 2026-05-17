package com.workitem.dto;

import lombok.Data;

/**
 * Screen Item响应
 */
@Data
public class ScreenItemResponse {
    private Long id;
    private Long fieldDefinitionId;
    private Long screenTabId;  // 添加 Tab ID 字段
    private String fieldKey;
    private String fieldName;
    private String fieldType;
    private Integer displayOrder;
}
