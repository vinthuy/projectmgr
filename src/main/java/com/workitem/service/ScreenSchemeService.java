package com.workitem.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.workitem.dto.*;
import com.workitem.entity.ScreenScheme;
import com.workitem.entity.ScreenSchemeIssueType;
import java.util.List;

/**
 * ScreenScheme服务接口
 */
public interface ScreenSchemeService extends IService<ScreenScheme> {
    
    /**
     * 获取屏幕方案列表
     */
    List<ScreenSchemeResponse> listSchemes(Long tenantId);
    
    /**
     * 获取方案详情（含类型映射）
     */
    ScreenSchemeDetailResponse getSchemeDetail(Long schemeId);
    
    /**
     * 创建屏幕方案
     */
    ScreenScheme createScheme(ScreenSchemeCreateRequest request, Long tenantId);
    
    /**
     * 更新屏幕方案
     */
    ScreenScheme updateScheme(Long schemeId, ScreenSchemeUpdateRequest request);
    
    /**
     * 删除屏幕方案
     */
    void deleteScheme(Long schemeId);
    
    /**
     * 添加类型映射
     */
    ScreenSchemeIssueType addMapping(Long schemeId, ScreenSchemeMappingRequest request);
    
    /**
     * 批量更新类型映射
     */
    void batchUpdateMappings(Long schemeId, List<ScreenSchemeMappingRequest> requests);
    
    /**
     * 删除类型映射
     */
    void deleteMapping(Long mappingId);
}
