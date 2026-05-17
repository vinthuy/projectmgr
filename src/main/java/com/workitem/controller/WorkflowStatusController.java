package com.workitem.controller;

import com.workitem.dto.WorkflowStatusCreateRequest;
import com.workitem.dto.WorkflowStatusResponse;
import com.workitem.dto.WorkflowStatusUpdateRequest;
import com.workitem.service.WorkflowStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-statuses")
@RequiredArgsConstructor
public class WorkflowStatusController {

    private final WorkflowStatusService workflowStatusService;

    /**
     * 获取所有工作流状态
     */
    @GetMapping
    public Result<List<WorkflowStatusResponse>> listAll(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        List<WorkflowStatusResponse> statuses = workflowStatusService.listAll(tenantId);
        return Result.success(statuses);
    }

    /**
     * 根据ID获取状态
     */
    @GetMapping("/{id}")
    public Result<WorkflowStatusResponse> getById(@PathVariable Long id) {
        try {
            WorkflowStatusResponse status = workflowStatusService.getById(id);
            return Result.success(status);
        } catch (RuntimeException e) {
            return Result.error(404, e.getMessage());
        }
    }

    /**
     * 根据分类获取状态
     */
    @GetMapping("/by-category/{category}")
    public Result<List<WorkflowStatusResponse>> getByCategory(
            @PathVariable String category,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        List<WorkflowStatusResponse> statuses = workflowStatusService.getByCategory(tenantId, category);
        return Result.success(statuses);
    }

    /**
     * 根据编码获取状态
     */
    @GetMapping("/code/{statusCode}")
    public Result<WorkflowStatusResponse> getByCode(
            @PathVariable String statusCode,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        WorkflowStatusResponse status = workflowStatusService.getByCode(tenantId, statusCode);
        if (status == null) {
            return Result.error(404, "Workflow status not found: " + statusCode);
        }
        return Result.success(status);
    }

    /**
     * 创建工作流状态
     */
    @PostMapping
    public Result<WorkflowStatusResponse> create(
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @Valid @RequestBody WorkflowStatusCreateRequest request) {
        try {
            WorkflowStatusResponse status = workflowStatusService.create(tenantId, request);
            return Result.success(status);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 更新工作流状态
     */
    @PutMapping("/{id}")
    public Result<WorkflowStatusResponse> update(
            @PathVariable Long id,
            @RequestBody WorkflowStatusUpdateRequest request) {
        try {
            WorkflowStatusResponse status = workflowStatusService.update(id, request);
            return Result.success(status);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除工作流状态
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            workflowStatusService.delete(id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
