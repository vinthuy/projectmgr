package com.workitem.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DynamicQueryRequest {
    private List<String> columns;
    private Map<String, Object> condition;
    private Integer page = 1;
    private Integer pageSize = 20;
}
