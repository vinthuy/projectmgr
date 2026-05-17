package com.workitem.controller;

import com.workitem.dto.*;
import com.workitem.service.WorkItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/work-items")
@RequiredArgsConstructor
public class WorkItemController {

    private final WorkItemService workItemService;

    @PostMapping
    public Result<WorkItemResponse> create(@Valid @RequestBody WorkItemCreateRequest request) {
        return Result.success(workItemService.create(request));
    }

    @PutMapping("/{id}")
    public Result<WorkItemResponse> update(@PathVariable Long id, 
                                           @RequestBody WorkItemUpdateRequest request) {
        return Result.success(workItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workItemService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<WorkItemResponse> getById(@PathVariable Long id) {
        return Result.success(workItemService.getById(id));
    }

    @GetMapping
    public Result<PageResponse<WorkItemResponse>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(workItemService.list(page, pageSize));
    }

    @PostMapping("/query")
    public Result<PageResponse<Map<String, Object>>> dynamicQuery(@RequestBody DynamicQueryRequest request) {
        return Result.success(workItemService.dynamicQuery(request));
    }

    @PatchMapping("/{id}/custom-fields")
    public Result<WorkItemResponse> updateCustomFields(
            @PathVariable Long id,
            @RequestBody Map<String, Object> customFields) {
        WorkItemUpdateRequest request = WorkItemUpdateRequest.builder()
                .customFields(customFields)
                .build();
        return Result.success(workItemService.update(id, request));
    }
}
