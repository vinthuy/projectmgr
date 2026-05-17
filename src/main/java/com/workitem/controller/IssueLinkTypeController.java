package com.workitem.controller;

import com.workitem.dto.IssueLinkTypeCreateRequest;
import com.workitem.dto.IssueLinkTypeResponse;
import com.workitem.dto.IssueLinkTypeUpdateRequest;
import com.workitem.service.IssueLinkTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/link-types")
@RequiredArgsConstructor
public class IssueLinkTypeController {

    private final IssueLinkTypeService linkTypeService;

    /**
     * 获取所有关系类型
     */
    @GetMapping
    public Result<List<IssueLinkTypeResponse>> listAll(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        List<IssueLinkTypeResponse> types = linkTypeService.listAll(tenantId);
        return Result.success(types);
    }

    /**
     * 根据ID获取关系类型
     */
    @GetMapping("/{id}")
    public Result<IssueLinkTypeResponse> getById(@PathVariable Long id) {
        try {
            IssueLinkTypeResponse type = linkTypeService.getById(id);
            return Result.success(type);
        } catch (RuntimeException e) {
            return Result.error(404, e.getMessage());
        }
    }

    /**
     * 创建关系类型
     */
    @PostMapping
    public Result<IssueLinkTypeResponse> create(
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @Valid @RequestBody IssueLinkTypeCreateRequest request) {
        try {
            IssueLinkTypeResponse type = linkTypeService.create(tenantId, request);
            return Result.success(type);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 更新关系类型
     */
    @PutMapping("/{id}")
    public Result<IssueLinkTypeResponse> update(
            @PathVariable Long id,
            @RequestBody IssueLinkTypeUpdateRequest request) {
        try {
            IssueLinkTypeResponse type = linkTypeService.update(id, request);
            return Result.success(type);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除关系类型
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            linkTypeService.delete(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
