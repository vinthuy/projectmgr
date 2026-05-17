package com.workitem.dto;

import lombok.Data;

import java.util.List;

/**
 * Screen Tab响应
 */
@Data
public class ScreenTabResponse {
    private Long id;
    private String tabName;
    private Integer displayOrder;
    private List<ScreenItemResponse> items;  // Tab中的字段
}
