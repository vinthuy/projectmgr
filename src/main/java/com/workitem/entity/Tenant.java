package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tenant")
public class Tenant {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String tenantKey;

    private String tenantName;

    private String description;

    private String status; // ACTIVE, INACTIVE, SUSPENDED

    private String licenseType; // FREE, STANDARD, ENTERPRISE

    private Integer maxUsers;

    private Integer maxProjects;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
