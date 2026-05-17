package com.workitem.controller;

import com.workitem.dto.*;
import com.workitem.entity.ScreenScheme;
import com.workitem.service.ScreenSchemeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ScreenScheme控制器
 */
@RestController
@RequestMapping("/api/v1/screen-schemes")
@RequiredArgsConstructor
public class ScreenSchemeController {
    
    private final ScreenSchemeService screenSchemeService;
    
    /**
     * 获取屏幕方案列表
     */
    @GetMapping
    public Result<List<ScreenSchemeResponse>> listSchemes() {
        Long tenantId = getCurrentTenantId();
        List<ScreenSchemeResponse> schemes = screenSchemeService.listSchemes(tenantId);
        return Result.success(schemes);
    }
    
    /**
     * 获取方案详情
     */
    @GetMapping("/{id}")
    public Result<ScreenSchemeDetailResponse> getSchemeDetail(@PathVariable Long id) {
        ScreenSchemeDetailResponse detail = screenSchemeService.getSchemeDetail(id);
        return Result.success(detail);
    }
    
    /**
     * 创建屏幕方案
     */
    @PostMapping
    public Result<ScreenScheme> createScheme(@RequestBody ScreenSchemeCreateRequest request) {
        Long tenantId = getCurrentTenantId();
        ScreenScheme scheme = screenSchemeService.createScheme(request, tenantId);
        return Result.success(scheme);
    }
    
    /**
     * 更新屏幕方案
     */
    @PutMapping("/{id}")
    public Result<ScreenScheme> updateScheme(
        @PathVariable Long id, 
        @RequestBody ScreenSchemeUpdateRequest request
    ) {
        ScreenScheme scheme = screenSchemeService.updateScheme(id, request);
        return Result.success(scheme);
    }
    
    /**
     * 删除屏幕方案
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteScheme(@PathVariable Long id) {
        screenSchemeService.deleteScheme(id);
        return Result.success();
    }
    
    /**
     * 添加类型映射
     */
    @PostMapping("/{schemeId}/mappings")
    public Result<Void> addMapping(
        @PathVariable Long schemeId,
        @RequestBody ScreenSchemeMappingRequest request
    ) {
        screenSchemeService.addMapping(schemeId, request);
        return Result.success();
    }
    
    /**
     * 批量更新类型映射
     */
    @PutMapping("/{schemeId}/mappings/batch")
    public Result<Void> batchUpdateMappings(
        @PathVariable Long schemeId,
        @RequestBody List<ScreenSchemeMappingRequest> requests
    ) {
        screenSchemeService.batchUpdateMappings(schemeId, requests);
        return Result.success();
    }
    
    /**
     * 删除类型映射
     */
    @DeleteMapping("/mappings/{mappingId}")
    public Result<Void> deleteMapping(@PathVariable Long mappingId) {
        screenSchemeService.deleteMapping(mappingId);
        return Result.success();
    }
    
    /**
     * 获取当前租户ID
     */
    private Long getCurrentTenantId() {
        // TODO: 从UserContext获取实际租户ID
        return 1L;
    }
}
