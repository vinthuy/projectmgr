package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.WorkItemTypeCreateRequest;
import com.workitem.dto.WorkItemTypeResponse;
import com.workitem.dto.WorkItemTypeUpdateRequest;
import com.workitem.entity.WorkItemType;
import com.workitem.mapper.WorkItemTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemTypeService extends ServiceImpl<WorkItemTypeMapper, WorkItemType> {

    /**
     * 获取所有工作项类型（按租户）
     */
    public List<WorkItemTypeResponse> listAll(Long tenantId) {
        LambdaQueryWrapper<WorkItemType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkItemType::getTenantId, tenantId)
                .orderByAsc(WorkItemType::getHierarchyLevel)
                .orderByAsc(WorkItemType::getTypeKey);

        List<WorkItemType> types = this.list(wrapper);

        return types.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据编码获取工作项类型（按租户）
     */
    public WorkItemTypeResponse getByCode(Long tenantId, String typeKey) {
        if (tenantId == null || typeKey == null || typeKey.isEmpty()) {
            log.debug("getByCode参数为空, tenantId: {}, typeKey: {}", tenantId, typeKey);
            return null;
        }
        
        try {
            LambdaQueryWrapper<WorkItemType> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WorkItemType::getTenantId, tenantId)
                    .eq(WorkItemType::getTypeKey, typeKey);

            WorkItemType type = this.getOne(wrapper);
            return type != null ? convertToResponse(type) : null;
        } catch (Exception e) {
            log.error("查询工作项类型失败, tenantId: {}, typeKey: {}", tenantId, typeKey, e);
            throw new RuntimeException("查询工作项类型失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建工作项类型
     */
    @Transactional
    public WorkItemTypeResponse create(Long tenantId, WorkItemTypeCreateRequest request) {
        log.debug("开始创建工作项类型, tenantId: {}, typeKey: {}", tenantId, request.getTypeKey());
        
        // 检查类型标识是否已存在
        if (request.getTypeKey() != null && !request.getTypeKey().isEmpty()) {
            WorkItemTypeResponse existing = getByCode(tenantId, request.getTypeKey());
            if (existing != null) {
                log.warn("类型标识已存在: {}", request.getTypeKey());
                throw new RuntimeException("类型标识已存在: " + request.getTypeKey());
            }
        }

        WorkItemType type = new WorkItemType();
        type.setTenantId(tenantId);
        type.setTypeKey(request.getTypeKey());
        type.setTypeName(request.getTypeName());
        type.setDescription(request.getDescription());
        type.setIcon(request.getIcon());
        type.setTypeCategory(request.getTypeCategory() != null ? request.getTypeCategory() : "STANDARD");
        type.setHierarchyLevel(request.getHierarchyLevel() != null ? request.getHierarchyLevel() : 0);
        type.setCreatedAt(LocalDateTime.now());
        type.setUpdatedAt(LocalDateTime.now());

        this.save(type);
        log.debug("工作项类型创建成功, id: {}, typeKey: {}", type.getId(), type.getTypeKey());
        return convertToResponse(type);
    }

    /**
     * 更新工作项类型
     */
    @Transactional
    public WorkItemTypeResponse update(Long id, WorkItemTypeUpdateRequest request) {
        log.debug("开始更新工作项类型, id: {}", id);
        
        WorkItemType type = this.baseMapper.selectById(id);
        if (type == null) {
            log.warn("工作项类型不存在: {}", id);
            throw new RuntimeException("工作项类型不存在: " + id);
        }

        // 只更新非空字段
        if (request.getTypeName() != null) {
            type.setTypeName(request.getTypeName());
        }
        if (request.getDescription() != null) {
            type.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            type.setIcon(request.getIcon());
        }
        if (request.getTypeCategory() != null) {
            type.setTypeCategory(request.getTypeCategory());
        }
        if (request.getHierarchyLevel() != null) {
            type.setHierarchyLevel(request.getHierarchyLevel());
        }

        type.setUpdatedAt(LocalDateTime.now());
        this.updateById(type);
        
        log.debug("工作项类型更新成功, id: {}", id);
        return convertToResponse(type);
    }

    /**
     * 删除工作项类型（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        log.debug("开始删除工作项类型, id: {}", id);
        
        WorkItemType type = this.baseMapper.selectById(id);
        if (type == null) {
            log.warn("工作项类型不存在: {}", id);
            throw new RuntimeException("工作项类型不存在: " + id);
        }

        this.removeById(id);
        
        log.debug("工作项类型删除成功, id: {}", id);
    }

    private WorkItemTypeResponse convertToResponse(WorkItemType type) {
        WorkItemTypeResponse response = new WorkItemTypeResponse();
        BeanUtils.copyProperties(type, response);
        return response;
    }
}
