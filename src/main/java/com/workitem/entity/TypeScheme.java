package com.workitem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 工作项类型方案实体
 */
@Data
@TableName("type_scheme")
public class TypeScheme {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String schemeKey;

    private String schemeName;

    private String description;

    private Boolean isSystem;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Boolean deleted;
}
