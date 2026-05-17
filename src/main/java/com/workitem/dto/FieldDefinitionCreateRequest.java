package com.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FieldDefinitionCreateRequest {
    @NotBlank(message = "字段标识不能为空")
    private String fieldKey;

    @NotBlank(message = "字段名称不能为空")
    private String fieldName;

    @NotBlank(message = "字段类型不能为空")
    private String fieldType; // TEXT, SELECT, MULTI_SELECT, NUMBER, DATE, USER, RICHTEXT, LABELS

    private String dataType; // text, number, datetime, user, array, html

    private String description;

    private Boolean required = false;

    private Object defaultValue;

    private Object options; // 用于SELECT/MULTI_SELECT的选项

    private String searcherKey;

    private String rendererKey;
}
