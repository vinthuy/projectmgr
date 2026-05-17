package com.workitem.controller;

import com.workitem.context.UserContextHolder;
import com.workitem.dto.IssueTypeScreenMappingRequest;
import com.workitem.dto.IssueTypeScreenResponse;
import com.workitem.service.IssueTypeScreenService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Issue Type Screen映射Controller
 */
@RestController
@RequestMapping("/api/v1/issue-type-screens")
@RequiredArgsConstructor
public class IssueTypeScreenController {

    private final IssueTypeScreenService issueTypeScreenService;

    /**
     * 获取所有映射
     */
    @GetMapping
    public Result<List<IssueTypeScreenResponse>> listAll() {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        List<IssueTypeScreenResponse> mappings = issueTypeScreenService.listAll(tenantId);
        return Result.success(mappings);
    }

    /**
     * 获取Issue Type的Screen
     */
    @GetMapping("/issue-types/{typeId}")
    public Result<IssueTypeScreenResponse> getScreenForIssueType(
            @PathVariable("typeId") Long issueTypeId,
            @RequestParam String operationType) {
        Long tenantId = UserContextHolder.getCurrentTenantId();
        IssueTypeScreenResponse mapping = issueTypeScreenService.getScreenForIssueType(
                tenantId, issueTypeId, operationType);
        return Result.success(mapping);
    }

    /**
     * 创建映射
     */
    @PostMapping
    public Result<Void> createMapping(@Validated @RequestBody IssueTypeScreenMappingRequest request) {
        issueTypeScreenService.createMapping(request);
        return Result.success(null);
    }

    /**
     * 更新映射
     */
    @PutMapping("/{id}")
    public Result<Void> updateMapping(@PathVariable Long id,
                                      @Validated @RequestBody IssueTypeScreenMappingRequest request) {
        issueTypeScreenService.updateMapping(id, request);
        return Result.success(null);
    }

    /**
     * 删除映射
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMapping(@PathVariable Long id) {
        issueTypeScreenService.deleteMapping(id);
        return Result.success(null);
    }
}
