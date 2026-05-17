package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.WorkflowStatusCreateRequest;
import com.workitem.dto.WorkflowStatusResponse;
import com.workitem.dto.WorkflowStatusUpdateRequest;
import com.workitem.entity.WorkflowStatus;
import com.workitem.mapper.WorkflowStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowStatusService extends ServiceImpl<WorkflowStatusMapper, WorkflowStatus> {

    private static final List<String> VALID_CATEGORIES = Arrays.asList("TO_DO", "IN_PROGRESS", "DONE");

    /**
     * 获取所有工作流状态（按租户）
     */
    public List<WorkflowStatusResponse> listAll(Long tenantId) {
        LambdaQueryWrapper<WorkflowStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowStatus::getTenantId, tenantId)
                .orderByAsc(WorkflowStatus::getDisplayOrder);

        List<WorkflowStatus> statuses = this.list(wrapper);

        return statuses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有工作流状态（默认租户）
     */
    public List<WorkflowStatusResponse> listAll() {
        return listAll(1L);
    }

    /**
     * 根据分类获取状态（按租户）
     */
    public List<WorkflowStatusResponse> getByCategory(Long tenantId, String category) {
        LambdaQueryWrapper<WorkflowStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowStatus::getTenantId, tenantId)
                .eq(WorkflowStatus::getCategory, category)
                .orderByAsc(WorkflowStatus::getDisplayOrder);

        List<WorkflowStatus> statuses = this.list(wrapper);

        return statuses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据分类获取状态（默认租户）
     */
    public List<WorkflowStatusResponse> getByCategory(String category) {
        return getByCategory(1L, category);
    }

    /**
     * 根据编码获取状态（按租户）
     */
    public WorkflowStatusResponse getByCode(Long tenantId, String statusCode) {
        LambdaQueryWrapper<WorkflowStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowStatus::getTenantId, tenantId)
                .eq(WorkflowStatus::getStatusCode, statusCode);

        WorkflowStatus status = this.getOne(wrapper);
        return status != null ? convertToResponse(status) : null;
    }

    /**
     * 根据编码获取状态（默认租户）
     */
    public WorkflowStatusResponse getByCode(String statusCode) {
        return getByCode(1L, statusCode);
    }

    /**
     * 创建工作流状态
     */
    @Transactional
    public WorkflowStatusResponse create(Long tenantId, WorkflowStatusCreateRequest request) {
        log.debug("开始创建工作流状态, tenantId: {}, statusCode: {}", tenantId, request.getStatusCode());
        
        validateCategory(request.getCategory());
        checkStatusCodeExists(tenantId, request.getStatusCode());

        WorkflowStatus status = new WorkflowStatus();
        status.setTenantId(tenantId);
        status.setStatusCode(request.getStatusCode());
        status.setStatusName(request.getStatusName());
        status.setCategory(request.getCategory());
        status.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        status.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        status.setCreatedAt(LocalDateTime.now());
        status.setUpdatedAt(LocalDateTime.now());

        this.save(status);
        log.debug("工作流状态创建成功, id: {}, statusCode: {}", status.getId(), status.getStatusCode());
        return convertToResponse(status);
    }

    /**
     * 更新工作流状态
     */
    @Transactional
    public WorkflowStatusResponse update(Long id, WorkflowStatusUpdateRequest request) {
        log.debug("开始更新工作流状态, id: {}", id);
        
        WorkflowStatus status = this.baseMapper.selectById(id);
        if (status == null) {
            throw new RuntimeException("工作流状态不存在: " + id);
        }

        // 只更新非空字段
        if (request.getStatusName() != null) {
            status.setStatusName(request.getStatusName());
        }
        if (request.getCategory() != null) {
            validateCategory(request.getCategory());
            status.setCategory(request.getCategory());
        }
        if (request.getDisplayOrder() != null) {
            status.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            status.setIsActive(request.getIsActive());
        }

        status.setUpdatedAt(LocalDateTime.now());
        this.updateById(status);
        
        log.debug("工作流状态更新成功, id: {}", id);
        return convertToResponse(status);
    }

    /**
     * 删除工作流状态（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        log.debug("开始删除工作流状态, id: {}", id);
        
        WorkflowStatus status = this.baseMapper.selectById(id);
        if (status == null) {
            throw new RuntimeException("工作流状态不存在: " + id);
        }

        this.removeById(id);
        
        log.debug("工作流状态删除成功, id: {}", id);
    }

    /**
     * 根据ID获取状态
     */
    public WorkflowStatusResponse getById(Long id) {
        WorkflowStatus status = this.baseMapper.selectById(id);
        if (status == null) {
            throw new RuntimeException("工作流状态不存在: " + id);
        }
        return convertToResponse(status);
    }

    /**
     * 验证状态分类
     */
    private void validateCategory(String category) {
        if (!VALID_CATEGORIES.contains(category)) {
            throw new RuntimeException("无效的状态分类: " + category + 
                    "。有效分类: " + VALID_CATEGORIES);
        }
    }

    /**
     * 检查状态编码是否已存在
     */
    private void checkStatusCodeExists(Long tenantId, String statusCode) {
        LambdaQueryWrapper<WorkflowStatus> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowStatus::getTenantId, tenantId)
                .eq(WorkflowStatus::getStatusCode, statusCode);
        
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("状态编码已存在: " + statusCode);
        }
    }

    private WorkflowStatusResponse convertToResponse(WorkflowStatus status) {
        WorkflowStatusResponse response = new WorkflowStatusResponse();
        BeanUtils.copyProperties(status, response);
        return response;
    }
}
