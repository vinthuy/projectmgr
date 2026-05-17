package com.workitem.controller;

import com.workitem.entity.WorkflowScheme;
import com.workitem.entity.WorkflowSchemeIssueType;
import com.workitem.service.WorkflowSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-schemes")
@RequiredArgsConstructor
public class WorkflowSchemeController {
    
    private final WorkflowSchemeService workflowSchemeService;
    
    @GetMapping
    public Result<List<WorkflowScheme>> listSchemes() {
        Long tenantId = getCurrentTenantId();
        List<WorkflowScheme> schemes = workflowSchemeService.listSchemes(tenantId);
        return Result.success(schemes);
    }
    
    @GetMapping("/{id}")
    public Result<List<WorkflowSchemeIssueType>> getSchemeDetail(@PathVariable Long id) {
        List<WorkflowSchemeIssueType> mappings = workflowSchemeService.getSchemeMappings(id);
        return Result.success(mappings);
    }
    
    @PostMapping
    public Result<WorkflowScheme> createScheme(@RequestBody WorkflowScheme scheme) {
        Long tenantId = getCurrentTenantId();
        WorkflowScheme created = workflowSchemeService.createScheme(scheme, tenantId);
        return Result.success(created);
    }
    
    @PutMapping("/{id}")
    public Result<WorkflowScheme> updateScheme(@PathVariable Long id, @RequestBody WorkflowScheme scheme) {
        WorkflowScheme updated = workflowSchemeService.updateScheme(id, scheme);
        return Result.success(updated);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        workflowSchemeService.deleteScheme(id);
        return Result.success();
    }
    
    @PutMapping("/{schemeId}/mappings/batch")
    public Result<Void> batchUpdateMappings(
        @PathVariable Long schemeId,
        @RequestBody List<WorkflowSchemeIssueType> mappings
    ) {
        workflowSchemeService.batchUpdateMappings(schemeId, mappings);
        return Result.success();
    }
    
    private Long getCurrentTenantId() {
        return 1L;
    }
}
