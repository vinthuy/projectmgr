package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("field_definition")
public class FieldDefinition {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String fieldKey; // 字段标识（唯一）

    private String fieldName; // 字段名称

    private String fieldType; // TEXT, SELECT, MULTI_SELECT, NUMBER, DATE, USER, etc.

    private String dataType; // text, number, datetime, user, array

    private String description; // 描述

    private Boolean required; // 是否必填

    @TableField(typeHandler = JacksonJsonTypeHandler.class)
    private Object defaultValue; // 默认值（JSON）

    @TableField(typeHandler = JacksonJsonTypeHandler.class)
    private Object options; // 选项（用于SELECT/MULTI_SELECT）

    private String searcherKey; // 搜索器类型

    private String rendererKey; // 渲染器类型

    private Boolean isSystem; // 是否系统字段

    private Boolean isGlobal; // 是否全局字段

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
