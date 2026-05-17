package com.workitem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workitem.entity.TypeScheme;
import com.workitem.entity.TypeSchemeIssueType;

import java.util.List;

public interface TypeSchemeService extends IService<TypeScheme> {
    
    List<TypeScheme> listSchemes(Long tenantId);
    
    TypeScheme createScheme(TypeScheme scheme, Long tenantId);
    
    TypeScheme updateScheme(Long id, TypeScheme scheme);
    
    void deleteScheme(Long id);
    
    List<TypeSchemeIssueType> getSchemeMappings(Long schemeId);
    
    void batchUpdateMappings(Long schemeId, List<TypeSchemeIssueType> mappings);
}
