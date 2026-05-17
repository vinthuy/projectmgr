package com.workitem.dto;

import lombok.Data;

@Data
public class IssueLinkTypeUpdateRequest {
    private String inwardName;
    private String outwardName;
    private String description;
    private Integer displayOrder;
}
