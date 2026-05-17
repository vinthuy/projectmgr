package com.workitem.dto;

import lombok.Data;

@Data
public class IssueLinkTypeResponse {
    private Long id;
    private String linkKey;
    private String inwardName;
    private String outwardName;
    private String description;
    private Boolean isSystem;
    private Integer displayOrder;
}
