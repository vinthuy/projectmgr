package com.workitem.dto;

import lombok.Data;

/**
 * Screen更新请求
 */
@Data
public class ScreenUpdateRequest {
    private String screenName;
    private String description;
}
