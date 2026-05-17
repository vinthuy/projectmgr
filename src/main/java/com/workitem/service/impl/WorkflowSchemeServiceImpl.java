package com.workitem.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.entity.WorkflowScheme;
import com.workitem.entity.WorkflowSchemeIssueType;
import com.workitem.mapper.WorkflowSchemeMapper;
import com.workitem.mapper.WorkflowSchemeIssueTypeMapper;
import com.workitem.service.WorkflowSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowSchemeServiceImpl extends ServiceImpl<WorkflowSchemeMapper, WorkflowScheme> implements WorkflowSchemeService {
    
    private final WorkflowSchemeIssueTypeMapper mappingMapper;
    
    @Override
    public List<WorkflowScheme> listSchemes(Long tenantId) {
        return lambdaQuery()
            .eq(WorkflowScheme::getTenantId, tenantId)
            .orderByAsc(WorkflowScheme::getIsSystem)
            .orderByDesc(WorkflowScheme::getCreatedAt)
            .list();
    }
    
    @Override
    @Transactional
    public WorkflowScheme createScheme(WorkflowScheme scheme, Long tenantId) {
        scheme.setTenantId(tenantId);
        scheme.setCreatedAt(LocalDateTime.now());
        scheme.setUpdatedAt(LocalDateTime.now());
        save(scheme);
        return scheme;
    }
    
    @Override
    @Transactional
    public WorkflowScheme updateScheme(Long id, WorkflowScheme scheme) {
        WorkflowScheme existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("方案不存在");
        }
        if (existing.getIsSystem()) {
            throw new RuntimeException("系统方案不可修改");
        }
        scheme.setId(id);
        scheme.setUpdatedAt(LocalDateTime.now());
        updateById(scheme);
        return getById(id);
    }
    
    @Override
    @Transactional
    public void deleteScheme(Long id) {
        WorkflowScheme scheme = getById(id);
        if (scheme == null) {
            throw new RuntimeException("方案不存在");
        }
        if (scheme.getIsSystem()) {
            throw new RuntimeException("系统方案不可删除");
        }
        removeById(id);
        mappingMapper.deleteBySchemeId(id);
    }
    
    @Override
    public List<WorkflowSchemeIssueType> getSchemeMappings(Long schemeId) {
        return mappingMapper.selectWithDetails(schemeId);
    }
    
    @Override
    @Transactional
    public void batchUpdateMappings(Long schemeId, List<WorkflowSchemeIssueType> mappings) {
        WorkflowScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("方案不存在");
        }
        
        mappingMapper.deleteBySchemeId(schemeId);
        
        for (WorkflowSchemeIssueType mapping : mappings) {
            mapping.setSchemeId(schemeId);
            mappingMapper.insert(mapping);
        }
    }
}
