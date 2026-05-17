package com.workitem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workitem.entity.WorkflowScheme;
import com.workitem.entity.WorkflowSchemeIssueType;

import java.util.List;

public interface WorkflowSchemeService extends IService<WorkflowScheme> {
    
    List<WorkflowScheme> listSchemes(Long tenantId);
    
    WorkflowScheme createScheme(WorkflowScheme scheme, Long tenantId);
    
    WorkflowScheme updateScheme(Long id, WorkflowScheme scheme);
    
    void deleteScheme(Long id);
    
    List<WorkflowSchemeIssueType> getSchemeMappings(Long schemeId);
    
    void batchUpdateMappings(Long schemeId, List<WorkflowSchemeIssueType> mappings);
}
