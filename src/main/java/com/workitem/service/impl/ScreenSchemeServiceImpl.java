package com.workitem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.*;
import com.workitem.entity.ScreenScheme;
import com.workitem.entity.ScreenSchemeIssueType;
import com.workitem.mapper.ScreenSchemeMapper;
import com.workitem.mapper.ScreenSchemeIssueTypeMapper;
import com.workitem.service.ScreenSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ScreenScheme服务实现
 */
@Service
@RequiredArgsConstructor
public class ScreenSchemeServiceImpl extends ServiceImpl<ScreenSchemeMapper, ScreenScheme> 
    implements ScreenSchemeService {
    
    private final ScreenSchemeIssueTypeMapper mappingMapper;
    
    @Override
    public List<ScreenSchemeResponse> listSchemes(Long tenantId) {
        LambdaQueryWrapper<ScreenScheme> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScreenScheme::getTenantId, tenantId)
               .orderByDesc(ScreenScheme::getCreatedAt);
        
        List<ScreenScheme> schemes = list(wrapper);
        
        return schemes.stream().map(scheme -> {
            ScreenSchemeResponse response = new ScreenSchemeResponse();
            response.setId(scheme.getId());
            response.setSchemeKey(scheme.getSchemeKey());
            response.setSchemeName(scheme.getSchemeName());
            response.setDescription(scheme.getDescription());
            response.setIsSystem(scheme.getIsSystem());
            response.setCreatedAt(scheme.getCreatedAt());
            response.setUpdatedAt(scheme.getUpdatedAt());
            
            // 统计关联的工作项类型数量
            Long count = mappingMapper.selectCount(
                new LambdaQueryWrapper<ScreenSchemeIssueType>()
                    .eq(ScreenSchemeIssueType::getSchemeId, scheme.getId())
            );
            response.setIssueTypeCount(count.intValue());
            
            return response;
        }).toList();
    }
    
    @Override
    public ScreenSchemeDetailResponse getSchemeDetail(Long schemeId) {
        ScreenScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("屏幕方案不存在");
        }
        
        ScreenSchemeDetailResponse response = new ScreenSchemeDetailResponse();
        response.setId(scheme.getId());
        response.setSchemeKey(scheme.getSchemeKey());
        response.setSchemeName(scheme.getSchemeName());
        response.setDescription(scheme.getDescription());
        response.setIsSystem(scheme.getIsSystem());
        response.setCreatedAt(scheme.getCreatedAt());
        response.setUpdatedAt(scheme.getUpdatedAt());
        
        // 查询类型映射详情
        List<ScreenSchemeIssueTypeResponse> mappings = mappingMapper.selectWithDetails(schemeId);
        response.setMappings(mappings);
        
        return response;
    }
    
    @Override
    @Transactional
    public ScreenScheme createScheme(ScreenSchemeCreateRequest request, Long tenantId) {
        // 检查schemeKey是否已存在
        Long count = count(new LambdaQueryWrapper<ScreenScheme>()
            .eq(ScreenScheme::getTenantId, tenantId)
            .eq(ScreenScheme::getSchemeKey, request.getSchemeKey()));
        
        if (count > 0) {
            throw new RuntimeException("方案Key已存在: " + request.getSchemeKey());
        }
        
        ScreenScheme scheme = new ScreenScheme();
        scheme.setTenantId(tenantId);
        scheme.setSchemeKey(request.getSchemeKey());
        scheme.setSchemeName(request.getSchemeName());
        scheme.setDescription(request.getDescription());
        scheme.setIsSystem(false);
        
        save(scheme);
        return scheme;
    }
    
    @Override
    @Transactional
    public ScreenScheme updateScheme(Long schemeId, ScreenSchemeUpdateRequest request) {
        ScreenScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("屏幕方案不存在");
        }
        
        if (scheme.getIsSystem()) {
            throw new RuntimeException("系统方案不可修改");
        }
        
        scheme.setSchemeName(request.getSchemeName());
        scheme.setDescription(request.getDescription());
        
        updateById(scheme);
        return scheme;
    }
    
    @Override
    @Transactional
    public void deleteScheme(Long schemeId) {
        ScreenScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("屏幕方案不存在");
        }
        
        if (scheme.getIsSystem()) {
            throw new RuntimeException("系统方案不可删除");
        }
        
        // 删除关联的映射
        mappingMapper.deleteBySchemeId(schemeId);
        
        // 删除方案
        removeById(schemeId);
    }
    
    @Override
    @Transactional
    public ScreenSchemeIssueType addMapping(Long schemeId, ScreenSchemeMappingRequest request) {
        ScreenScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("屏幕方案不存在");
        }
        
        // 检查是否已存在映射
        Long count = mappingMapper.selectCount(
            new LambdaQueryWrapper<ScreenSchemeIssueType>()
                .eq(ScreenSchemeIssueType::getSchemeId, schemeId)
                .eq(ScreenSchemeIssueType::getIssueTypeId, request.getIssueTypeId())
        );
        
        if (count > 0) {
            throw new RuntimeException("该工作项类型已存在映射");
        }
        
        ScreenSchemeIssueType mapping = new ScreenSchemeIssueType();
        mapping.setSchemeId(schemeId);
        mapping.setIssueTypeId(request.getIssueTypeId());
        mapping.setCreateScreenId(request.getCreateScreenId());
        mapping.setEditScreenId(request.getEditScreenId());
        mapping.setViewScreenId(request.getViewScreenId());
        
        mappingMapper.insert(mapping);
        return mapping;
    }
    
    @Override
    @Transactional
    public void batchUpdateMappings(Long schemeId, List<ScreenSchemeMappingRequest> requests) {
        ScreenScheme scheme = getById(schemeId);
        if (scheme == null) {
            throw new RuntimeException("屏幕方案不存在");
        }
        
        // 删除旧映射
        mappingMapper.deleteBySchemeId(schemeId);
        
        // 插入新映射
        for (ScreenSchemeMappingRequest request : requests) {
            ScreenSchemeIssueType mapping = new ScreenSchemeIssueType();
            mapping.setSchemeId(schemeId);
            mapping.setIssueTypeId(request.getIssueTypeId());
            mapping.setCreateScreenId(request.getCreateScreenId());
            mapping.setEditScreenId(request.getEditScreenId());
            mapping.setViewScreenId(request.getViewScreenId());
            mappingMapper.insert(mapping);
        }
    }
    
    @Override
    @Transactional
    public void deleteMapping(Long mappingId) {
        mappingMapper.deleteById(mappingId);
    }
}
