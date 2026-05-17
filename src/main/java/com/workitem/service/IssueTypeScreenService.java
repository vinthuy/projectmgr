package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.IssueTypeScreenMappingRequest;
import com.workitem.dto.IssueTypeScreenResponse;
import com.workitem.entity.IssueTypeScreen;
import com.workitem.entity.Screen;
import com.workitem.entity.WorkItemType;
import com.workitem.mapper.IssueTypeScreenMapper;
import com.workitem.mapper.ScreenMapper;
import com.workitem.mapper.WorkItemTypeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Issue Type Screen映射服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTypeScreenService extends ServiceImpl<IssueTypeScreenMapper, IssueTypeScreen> {

    private final ScreenMapper screenMapper;
    private final WorkItemTypeMapper workItemTypeMapper;

    /**
     * 获取所有映射
     */
    public List<IssueTypeScreenResponse> listAll(Long tenantId) {
        log.debug("查询所有Issue Type Screen映射, tenantId: {}", tenantId);
        
        LambdaQueryWrapper<IssueTypeScreen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueTypeScreen::getTenantId, tenantId)
                .orderByAsc(IssueTypeScreen::getWorkItemTypeId)
                .orderByAsc(IssueTypeScreen::getOperationType);
        
        List<IssueTypeScreen> mappings = this.list(wrapper);
        
        return mappings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取Issue Type的Screen
     */
    public IssueTypeScreenResponse getScreenForIssueType(Long tenantId, Long issueTypeId, String operationType) {
        log.debug("查询Issue Type的Screen, issueTypeId: {}, operationType: {}", issueTypeId, operationType);
        
        LambdaQueryWrapper<IssueTypeScreen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueTypeScreen::getTenantId, tenantId)
                .eq(IssueTypeScreen::getWorkItemTypeId, issueTypeId)
                .eq(IssueTypeScreen::getOperationType, operationType);
        
        IssueTypeScreen mapping = this.getOne(wrapper);
        if (mapping == null) {
            return null;
        }
        
        return convertToResponse(mapping);
    }

    /**
     * 创建映射
     */
    @Transactional
    public void createMapping(IssueTypeScreenMappingRequest request) {
        log.debug("开始创建映射, issueTypeId: {}, operationType: {}", 
                request.getWorkItemTypeId(), request.getOperationType());
        
        // 验证操作类型
        validateOperationType(request.getOperationType());
        
        // 检查唯一性
        checkMappingExists(request.getWorkItemTypeId(), request.getOperationType());
        
        // 创建映射
        IssueTypeScreen mapping = new IssueTypeScreen();
        mapping.setTenantId(1L); // TODO: 从上下文获取
        mapping.setWorkItemTypeId(request.getWorkItemTypeId());
        mapping.setScreenId(request.getScreenId());
        mapping.setOperationType(request.getOperationType());
        mapping.setCreatedAt(LocalDateTime.now());
        mapping.setUpdatedAt(LocalDateTime.now());
        
        this.save(mapping);
        
        log.debug("映射创建成功");
    }

    /**
     * 更新映射
     */
    @Transactional
    public void updateMapping(Long id, IssueTypeScreenMappingRequest request) {
        log.debug("开始更新映射, id: {}", id);
        
        IssueTypeScreen mapping = this.baseMapper.selectById(id);
        if (mapping == null) {
            throw new RuntimeException("映射不存在: " + id);
        }
        
        // 验证操作类型
        validateOperationType(request.getOperationType());
        
        mapping.setWorkItemTypeId(request.getWorkItemTypeId());
        mapping.setScreenId(request.getScreenId());
        mapping.setOperationType(request.getOperationType());
        mapping.setUpdatedAt(LocalDateTime.now());
        
        this.updateById(mapping);
        
        log.debug("映射更新成功");
    }

    /**
     * 删除映射
     */
    @Transactional
    public void deleteMapping(Long id) {
        log.debug("开始删除映射, id: {}", id);
        
        IssueTypeScreen mapping = this.baseMapper.selectById(id);
        if (mapping == null) {
            throw new RuntimeException("映射不存在: " + id);
        }
        
        this.removeById(id);
        log.debug("映射删除成功");
    }

    // ==================== 私有方法 ====================

    private void validateOperationType(String operationType) {
        if (!"CREATE".equals(operationType) && 
            !"EDIT".equals(operationType) && 
            !"VIEW".equals(operationType)) {
            throw new RuntimeException("无效的操作类型: " + operationType);
        }
    }

    private void checkMappingExists(Long issueTypeId, String operationType) {
        LambdaQueryWrapper<IssueTypeScreen> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IssueTypeScreen::getWorkItemTypeId, issueTypeId)
                .eq(IssueTypeScreen::getOperationType, operationType);
        
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该Issue Type的" + operationType + "操作已配置Screen");
        }
    }

    private IssueTypeScreenResponse convertToResponse(IssueTypeScreen mapping) {
        IssueTypeScreenResponse response = new IssueTypeScreenResponse();
        BeanUtils.copyProperties(mapping, response);
        
        // 查询Issue Type名称
        WorkItemType workItemType = workItemTypeMapper.selectById(mapping.getWorkItemTypeId());
        if (workItemType != null) {
            response.setWorkItemTypeName(workItemType.getTypeName());
        }
        
        // 查询Screen名称
        Screen screen = screenMapper.selectById(mapping.getScreenId());
        if (screen != null) {
            response.setScreenName(screen.getScreenName());
        }
        
        return response;
    }
}
