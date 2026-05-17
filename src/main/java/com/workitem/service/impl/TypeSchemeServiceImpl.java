package com.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.entity.TypeScheme;
import com.workitem.entity.TypeSchemeIssueType;
import com.workitem.mapper.TypeSchemeMapper;
import com.workitem.mapper.TypeSchemeIssueTypeMapper;
import com.workitem.service.TypeSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TypeSchemeServiceImpl extends ServiceImpl<TypeSchemeMapper, TypeScheme> implements TypeSchemeService {
    
    private final TypeSchemeIssueTypeMapper mappingMapper;
    
    @Override
    public List<TypeScheme> listSchemes(Long tenantId) {
        return lambdaQuery()
            .eq(TypeScheme::getTenantId, tenantId)
            .orderByAsc(TypeScheme::getIsSystem)
            .orderByDesc(TypeScheme::getCreatedAt)
            .list();
    }
    
    @Override
    @Transactional
    public TypeScheme createScheme(TypeScheme scheme, Long tenantId) {
        scheme.setTenantId(tenantId);
        scheme.setCreatedAt(LocalDateTime.now());
        scheme.setUpdatedAt(LocalDateTime.now());
        save(scheme);
        return scheme;
    }
    
    @Override
    @Transactional
    public TypeScheme updateScheme(Long id, TypeScheme scheme) {
        TypeScheme existing = getById(id);
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
        TypeScheme scheme = getById(id);
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
    public List<TypeSchemeIssueType> getSchemeMappings(Long schemeId) {
        return mappingMapper.selectWithDetails(schemeId);
    }
    
    @Override
    @Transactional
    public void batchUpdateMappings(Long schemeId, List<TypeSchemeIssueType> mappings) {
        TypeScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("方案不存在");
        }
        
        mappingMapper.deleteBySchemeId(schemeId);
        
        for (int i = 0; i < mappings.size(); i++) {
            TypeSchemeIssueType mapping = mappings.get(i);
            mapping.setSchemeId(schemeId);
            mapping.setDisplayOrder(i);
            mappingMapper.insert(mapping);
        }
    }
}
