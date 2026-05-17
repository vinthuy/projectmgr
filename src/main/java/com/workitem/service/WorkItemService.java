package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.*;
import com.workitem.entity.FieldDefinition;
import com.workitem.entity.WorkItem;
import com.workitem.mapper.FieldDefinitionMapper;
import com.workitem.mapper.WorkItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemService extends ServiceImpl<WorkItemMapper, WorkItem> {

    private final WorkItemMapper workItemMapper;
    private final FieldDefinitionMapper fieldDefinitionMapper;

    @Transactional
    public WorkItemResponse create(WorkItemCreateRequest request) {
        WorkItem workItem = new WorkItem();
        workItem.setTitle(request.getTitle());
        workItem.setDescription(request.getDescription());
        workItem.setStatus(request.getStatus() != null ? request.getStatus() : "OPEN");
        workItem.setPriority(request.getPriority() != null ? request.getPriority() : "MEDIUM");
        workItem.setAssignee(request.getAssignee());
        workItem.setCustomFields(request.getCustomFields() != null ? request.getCustomFields() : new HashMap<>());

        this.save(workItem);
        return toResponse(workItem);
    }

    @Transactional
    public WorkItemResponse update(Long id, WorkItemUpdateRequest request) {
        WorkItem workItem = this.baseMapper.selectById(id);
        if (workItem == null) {
            throw new RuntimeException("Work item not found");
        }

        if (request.getTitle() != null) {
            workItem.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            workItem.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            workItem.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            workItem.setPriority(request.getPriority());
        }
        if (request.getAssignee() != null) {
            workItem.setAssignee(request.getAssignee());
        }
        if (request.getCustomFields() != null) {
            Map<String, Object> currentFields = workItem.getCustomFields();
            if (currentFields == null) {
                currentFields = new HashMap<>();
            }
            currentFields.putAll(request.getCustomFields());
            workItem.setCustomFields(currentFields);
        }

        this.updateById(workItem);
        return toResponse(workItem);
    }

    @Transactional
    public void delete(Long id) {
        WorkItem workItem = this.baseMapper.selectById(id);
        if (workItem == null) {
            throw new RuntimeException("Work item not found");
        }
        this.removeById(id);
    }

    public WorkItemResponse getById(Long id) {
        WorkItem workItem = this.baseMapper.selectById(id);
        if (workItem == null) {
            throw new RuntimeException("Work item not found");
        }
        return toResponse(workItem);
    }

    public PageResponse<WorkItemResponse> list(Integer page, Integer pageSize) {
        Page<WorkItem> pageParam = new Page<>(page, pageSize);
        Page<WorkItem> result = this.page(pageParam, new LambdaQueryWrapper<>());

        List<WorkItemResponse> records = result.getRecords().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.of(records, result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    public PageResponse<Map<String, Object>> dynamicQuery(DynamicQueryRequest request) {
        List<String> columns = request.getColumns();
        if (columns == null || columns.isEmpty()) {
            columns = Arrays.asList("title", "status", "priority", "assignee");
        }

        int page = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;

        Map<String, Object> condition = request.getCondition();
        List<Map<String, Object>> records = workItemMapper.selectDynamicColumns(columns, condition, page, pageSize);
        long total = workItemMapper.countDynamicColumns(condition);

        return PageResponse.of(records, total, page, pageSize);
    }

    private WorkItemResponse toResponse(WorkItem workItem) {
        WorkItemResponse response = new WorkItemResponse();
        response.setId(workItem.getId());
        response.setTitle(workItem.getTitle());
        response.setDescription(workItem.getDescription());
        response.setStatus(workItem.getStatus());
        response.setPriority(workItem.getPriority());
        response.setAssignee(workItem.getAssignee());
        response.setCustomFields(workItem.getCustomFields());
        response.setCreatedAt(workItem.getCreatedAt());
        response.setUpdatedAt(workItem.getUpdatedAt());

        LambdaQueryWrapper<FieldDefinition> wrapper = new LambdaQueryWrapper<>();
        List<FieldDefinition> definitions = fieldDefinitionMapper.selectList(wrapper);
        response.setAvailableFields(definitions.stream()
                .map(this::toFieldResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private FieldDefinitionResponse toFieldResponse(FieldDefinition field) {
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
