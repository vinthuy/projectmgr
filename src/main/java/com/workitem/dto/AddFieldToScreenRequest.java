package com.workitem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 添加字段到Screen请求
 */
@Data
public class AddFieldToScreenRequest {
    @NotNull(message = "字段ID不能为空")
    private Long fieldDefinitionId;
    
    private Long screenTabId;  // 可选，不填则添加到默认Tab
    
    private Integer displayOrder;
}
