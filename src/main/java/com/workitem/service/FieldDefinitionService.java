package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.FieldDefinitionCreateRequest;
import com.workitem.dto.FieldDefinitionUpdateRequest;
import com.workitem.dto.FieldDefinitionResponse;
import com.workitem.entity.FieldDefinition;
import com.workitem.mapper.FieldDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FieldDefinitionService extends ServiceImpl<FieldDefinitionMapper, FieldDefinition> {

    private static final List<String> VALID_FIELD_TYPES = Arrays.asList(
            "TEXT", "NUMBER", "BOOLEAN", "DATE", "SELECT", "MULTI_SELECT", 
            "USER", "RICHTEXT", "LABELS", "DATETIME"
    );

    @Transactional
    public FieldDefinitionResponse create(Long tenantId, FieldDefinitionCreateRequest request) {
        log.debug("开始创建字段定义, tenantId: {}, fieldKey: {}", tenantId, request.getFieldKey());
        
        validateFieldType(request.getFieldType());
        checkFieldKeyExists(tenantId, request.getFieldKey());

        // 自动设置dataType
        String dataType = request.getDataType();
        if (dataType == null || dataType.isEmpty()) {
            dataType = inferDataType(request.getFieldType());
        }

        FieldDefinition field = new FieldDefinition();
        field.setTenantId(tenantId);
        field.setFieldKey(request.getFieldKey());
        field.setFieldName(request.getFieldName());
        field.setFieldType(request.getFieldType());
        field.setDataType(dataType);
        field.setDescription(request.getDescription());
        field.setRequired(request.getRequired() != null ? request.getRequired() : false);
        field.setDefaultValue(request.getDefaultValue());
        field.setOptions(request.getOptions());
        field.setSearcherKey(request.getSearcherKey());
        field.setRendererKey(request.getRendererKey());
        field.setIsSystem(false);
        field.setIsGlobal(false);

        this.save(field);
        log.debug("字段定义创建成功, id: {}, fieldKey: {}", field.getId(), field.getFieldKey());
        return toResponse(field);
    }

    public FieldDefinitionResponse create(FieldDefinitionCreateRequest request) {
        return create(1L, request);
    }

    @Transactional
    public FieldDefinitionResponse update(Long id, FieldDefinitionUpdateRequest request) {
        log.debug("开始更新字段定义, id: {}", id);
        
        FieldDefinition field = this.baseMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段定义不存在: " + id);
        }

        // 系统字段不允许修改某些属性
        if (field.getIsSystem() != null && field.getIsSystem()) {
            log.warn("尝试修改系统字段: {}", field.getFieldKey());
            throw new RuntimeException("系统字段不允许修改");
        }

        // 只更新非空字段
        if (request.getFieldName() != null) {
            field.setFieldName(request.getFieldName());
        }
        if (request.getDescription() != null) {
            field.setDescription(request.getDescription());
        }
        if (request.getRequired() != null) {
            field.setRequired(request.getRequired());
        }
        if (request.getDefaultValue() != null) {
            field.setDefaultValue(request.getDefaultValue());
        }
        if (request.getOptions() != null) {
            field.setOptions(request.getOptions());
        }
        if (request.getSearcherKey() != null) {
            field.setSearcherKey(request.getSearcherKey());
        }
        if (request.getRendererKey() != null) {
            field.setRendererKey(request.getRendererKey());
        }

        this.updateById(field);
        log.debug("字段定义更新成功, id: {}", id);
        return toResponse(field);
    }

    @Transactional
    public void delete(Long id) {
        log.debug("开始删除字段定义, id: {}", id);
        
        FieldDefinition field = this.baseMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段定义不存在: " + id);
        }

        // 系统字段不允许删除
        if (field.getIsSystem() != null && field.getIsSystem()) {
            log.warn("尝试删除系统字段: {}", field.getFieldKey());
            throw new RuntimeException("系统字段不允许删除");
        }

        this.removeById(id);
        log.debug("字段定义删除成功, id: {}", id);
    }

    public FieldDefinitionResponse getById(Long id) {
        FieldDefinition field = this.baseMapper.selectById(id);
        if (field == null) {
            throw new RuntimeException("字段定义不存在: " + id);
        }
        return toResponse(field);
    }

    public List<FieldDefinitionResponse> listByTenant(Long tenantId) {
        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldDefinition::getTenantId, tenantId);
        
        return this.list(wrapper).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<FieldDefinitionResponse> listByTenant() {
        return listByTenant(1L);
    }

    private void validateFieldType(String fieldType) {
        if (!VALID_FIELD_TYPES.contains(fieldType)) {
            throw new RuntimeException("Invalid field type: " + fieldType + 
                    ". Valid types: " + VALID_FIELD_TYPES);
        }
    }

    private void checkFieldKeyExists(Long tenantId, String fieldKey) {
        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FieldDefinition::getTenantId, tenantId)
                .eq(FieldDefinition::getFieldKey, fieldKey);
        
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("字段标识已存在: " + fieldKey);
        }
    }

    /**
     * 根据字段类型推断数据类型
     */
    private String inferDataType(String fieldType) {
        return switch (fieldType.toUpperCase()) {
            case "TEXT", "SELECT" -> "text";
            case "NUMBER" -> "number";
            case "DATE", "DATETIME" -> "datetime";
            case "USER" -> "user";
            case "MULTI_SELECT", "LABELS" -> "array";
            case "RICHTEXT" -> "html";
            case "BOOLEAN" -> "boolean";
            default -> "text";
        };
    }

    private FieldDefinitionResponse toResponse(FieldDefinition field) {
        FieldDefinitionResponse response = new FieldDefinitionResponse();
        response.setId(field.getId());
        response.setTenantId(field.getTenantId());
        response.setFieldKey(field.getFieldKey());
        response.setFieldName(field.getFieldName());
        response.setFieldType(field.getFieldType());
        response.setDataType(field.getDataType());
        response.setDescription(field.getDescription());
        response.setRequired(field.getRequired());
        response.setDefaultValue(field.getDefaultValue());
        response.setOptions(field.getOptions());
        response.setSearcherKey(field.getSearcherKey());
        response.setRendererKey(field.getRendererKey());
        response.setIsSystem(field.getIsSystem());
        response.setIsGlobal(field.getIsGlobal());
        response.setCreatedAt(field.getCreatedAt());
        response.setUpdatedAt(field.getUpdatedAt());
        return response;
    }
}
