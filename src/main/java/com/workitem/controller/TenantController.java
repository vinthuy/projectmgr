package com.workitem.controller;

import com.workitem.dto.TenantCreateRequest;
import com.workitem.dto.TenantResponse;
import com.workitem.dto.TenantUpdateRequest;
import com.workitem.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * 获取所有租户
     */
    @GetMapping
    public Result<List<TenantResponse>> listAll() {
        List<TenantResponse> tenants = tenantService.listAll();
        return Result.success(tenants);
    }

    /**
     * 根据ID获取租户
     */
    @GetMapping("/{id}")
    public Result<TenantResponse> getById(@PathVariable Long id) {
        TenantResponse tenant = tenantService.getById(id);
        if (tenant == null) {
            return Result.error(404, "Tenant not found: " + id);
        }
        return Result.success(tenant);
    }

    /**
     * 根据租户Key获取租户
     */
    @GetMapping("/key/{tenantKey}")
    public Result<TenantResponse> getByKey(@PathVariable String tenantKey) {
        TenantResponse tenant = tenantService.getByKey(tenantKey);
        if (tenant == null) {
            return Result.error(404, "Tenant not found: " + tenantKey);
        }
        return Result.success(tenant);
    }

    /**
     * 创建租户
     */
    @PostMapping
    public Result<TenantResponse> create(@Valid @RequestBody TenantCreateRequest request) {
        try {
            TenantResponse tenant = tenantService.create(request);
            return Result.success(tenant);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 更新租户
     */
    @PutMapping("/{id}")
    public Result<TenantResponse> update(@PathVariable Long id,
                                         @RequestBody TenantUpdateRequest request) {
        try {
            TenantResponse tenant = tenantService.update(id, request);
            return Result.success(tenant);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            tenantService.delete(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
