package com.workitem.controller;

import com.workitem.context.UserContextHolder;
import com.workitem.dto.WorkItemTypeCreateRequest;
import com.workitem.dto.WorkItemTypeResponse;
import com.workitem.dto.WorkItemTypeUpdateRequest;
import com.workitem.service.WorkItemTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/work-item-types")
@RequiredArgsConstructor
public class WorkItemTypeController {

    private final WorkItemTypeService workItemTypeService;

    /**
     * 获取所有工作项类型
     */
    @GetMapping
    public Result<List<WorkItemTypeResponse>> listAll() {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        List<WorkItemTypeResponse> types = workItemTypeService.listAll(tenantId);
        return Result.success(types);
    }

    /**
     * 根据编码获取工作项类型
     */
    @GetMapping("/{typeCode}")
    public Result<WorkItemTypeResponse> getByCode(@PathVariable String typeCode) {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        WorkItemTypeResponse type = workItemTypeService.getByCode(tenantId, typeCode);
        if (type == null) {
            return Result.error(404, "Work item type not found: " + typeCode);
        }
        return Result.success(type);
    }

    /**
     * 创建工作项类型
     */
    @PostMapping
    public Result<WorkItemTypeResponse> create(@Valid @RequestBody WorkItemTypeCreateRequest request) {
        try {
            Long tenantId = UserContextHolder.getCurrentTenantId();
            log.info("创建工作项类型, tenantId: {}, typeKey: {}", tenantId, request.getTypeKey());
            WorkItemTypeResponse type = workItemTypeService.create(tenantId, request);
            log.info("工作项类型创建成功, id: {}", type.getId());
            return Result.success(type);
        } catch (Exception e) {
            log.error("创建工作项类型失败", e);
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 更新工作项类型
     */
    @PutMapping("/{id}")
    public Result<WorkItemTypeResponse> update(
            @PathVariable Long id,
            @RequestBody WorkItemTypeUpdateRequest request) {
        try {
            log.info("更新工作项类型, id: {}", id);
            WorkItemTypeResponse type = workItemTypeService.update(id, request);
            log.info("工作项类型更新成功, id: {}", id);
            return Result.success(type);
        } catch (Exception e) {
            log.error("更新工作项类型失败, id: {}", id, e);
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除工作项类型
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            log.info("删除工作项类型, id: {}", id);
            workItemTypeService.delete(id);
            log.info("工作项类型删除成功, id: {}", id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除工作项类型失败, id: {}", id, e);
            return Result.error(400, e.getMessage());
        }
    }
}
