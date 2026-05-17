package com.workitem.controller;

import com.workitem.entity.TypeScheme;
import com.workitem.entity.TypeSchemeIssueType;
import com.workitem.service.TypeSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/type-schemes")
@RequiredArgsConstructor
public class TypeSchemeController {
    
    private final TypeSchemeService typeSchemeService;
    
    @GetMapping
    public Result<List<TypeScheme>> listSchemes() {
        Long tenantId = getCurrentTenantId();
        List<TypeScheme> schemes = typeSchemeService.listSchemes(tenantId);
        return Result.success(schemes);
    }
    
    @GetMapping("/{id}")
    public Result<List<TypeSchemeIssueType>> getSchemeDetail(@PathVariable Long id) {
        List<TypeSchemeIssueType> mappings = typeSchemeService.getSchemeMappings(id);
        return Result.success(mappings);
    }
    
    @PostMapping
    public Result<TypeScheme> createScheme(@RequestBody TypeScheme scheme) {
        Long tenantId = getCurrentTenantId();
        TypeScheme created = typeSchemeService.createScheme(scheme, tenantId);
        return Result.success(created);
    }
    
    @PutMapping("/{id}")
    public Result<TypeScheme> updateScheme(@PathVariable Long id, @RequestBody TypeScheme scheme) {
        TypeScheme updated = typeSchemeService.updateScheme(id, scheme);
        return Result.success(updated);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        typeSchemeService.deleteScheme(id);
        return Result.success();
    }
    
    @PutMapping("/{schemeId}/mappings/batch")
    public Result<Void> batchUpdateMappings(
        @PathVariable Long schemeId,
        @RequestBody List<TypeSchemeIssueType> mappings
    ) {
        typeSchemeService.batchUpdateMappings(schemeId, mappings);
        return Result.success();
    }
    
    private Long getCurrentTenantId() {
        return 1L;
    }
}
