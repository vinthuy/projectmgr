package com.workitem.controller;

import com.workitem.context.UserContextHolder;
import com.workitem.dto.*;
import com.workitem.service.ScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Screen管理Controller
 */
@RestController
@RequestMapping("/api/v1/screens")
@RequiredArgsConstructor
public class ScreenController {

    private final ScreenService screenService;

    /**
     * 获取所有Screen列表
     */
    @GetMapping
    public Result<List<ScreenResponse>> listAll() {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        List<ScreenResponse> screens = screenService.listAll(tenantId);
        return Result.success(screens);
    }

    /**
     * 获取Screen详情
     */
    @GetMapping("/{id}")
    public Result<ScreenResponse> getById(@PathVariable Long id) {
        ScreenResponse screen = screenService.getById(id);
        return Result.success(screen);
    }

    /**
     * 创建Screen
     */
    @PostMapping
    public Result<ScreenResponse> create(@Validated @RequestBody ScreenCreateRequest request) {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        ScreenResponse screen = screenService.create(tenantId, request);
        return Result.success(screen);
    }

    /**
     * 更新Screen
     */
    @PutMapping("/{id}")
    public Result<ScreenResponse> update(@PathVariable Long id, 
                                         @Validated @RequestBody ScreenUpdateRequest request) {
        ScreenResponse screen = screenService.update(id, request);
        return Result.success(screen);
    }

    /**
     * 删除Screen
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        screenService.delete(id);
        return Result.success(null);
    }

    /**
     * 添加Tab
     */
    @PostMapping("/{screenId}/tabs")
    public Result<ScreenTabResponse> addTab(@PathVariable Long screenId,
                                            @RequestBody Map<String, String> request) {
        String tabName = request.get("tabName");
        ScreenTabResponse tab = screenService.addTab(screenId, tabName);
        return Result.success(tab);
    }

    /**
     * 更新Tab
     */
    @PutMapping("/tabs/{tabId}")
    public Result<Void> updateTab(@PathVariable Long tabId,
                                  @RequestBody Map<String, String> request) {
        String tabName = request.get("tabName");
        screenService.updateTab(tabId, tabName);
        return Result.success(null);
    }

    /**
     * 删除Tab
     */
    @DeleteMapping("/tabs/{tabId}")
    public Result<Void> deleteTab(@PathVariable Long tabId) {
        screenService.deleteTab(tabId);
        return Result.success(null);
    }

    /**
     * 调整Tab顺序
     */
    @PutMapping("/{screenId}/tabs/reorder")
    public Result<Void> reorderTabs(@PathVariable Long screenId,
                                    @RequestBody List<Long> tabIds) {
        screenService.reorderTabs(screenId, tabIds);
        return Result.success(null);
    }

    /**
     * 添加字段到Screen
     */
    @PostMapping("/{screenId}/items")
    public Result<ScreenItemResponse> addField(@PathVariable Long screenId,
                                               @Validated @RequestBody AddFieldToScreenRequest request) {
        ScreenItemResponse item = screenService.addField(screenId, request);
        return Result.success(item);
    }

    /**
     * 从Screen移除字段
     */
    @DeleteMapping("/items/{itemId}")
    public Result<Void> removeField(@PathVariable Long itemId) {
        screenService.removeField(itemId);
        return Result.success(null);
    }

    /**
     * 调整字段顺序
     */
    @PutMapping("/{screenId}/items/reorder")
    public Result<Void> reorderFields(@PathVariable Long screenId,
                                      @RequestBody List<Long> itemIds) {
        screenService.reorderFields(screenId, itemIds);
        return Result.success(null);
    }
}
