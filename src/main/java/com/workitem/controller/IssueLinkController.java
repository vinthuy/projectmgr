package com.workitem.controller;

import com.workitem.dto.IssueLinkCreateRequest;
import com.workitem.dto.IssueLinkResponse;
import com.workitem.service.IssueLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/work-items/{itemId}/links")
@RequiredArgsConstructor
public class IssueLinkController {

    private final IssueLinkService linkService;

    /**
     * 获取工作项的所有关系
     */
    @GetMapping
    public Result<List<IssueLinkResponse>> getLinks(
            @PathVariable Long itemId,
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        List<IssueLinkResponse> links = linkService.getLinksByItemId(tenantId, itemId);
        return Result.success(links);
    }

    /**
     * 创建工作项关系
     */
    @PostMapping
    public Result<IssueLinkResponse> createLink(
            @PathVariable Long itemId,
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @RequestParam(required = false, defaultValue = "1") Long userId,
            @Valid @RequestBody IssueLinkCreateRequest request) {
        try {
            // 确保sourceItemId与路径参数一致
            if (!request.getSourceItemId().equals(itemId)) {
                return Result.error(400, "源工作项ID与路径参数不匹配");
            }
            
            IssueLinkResponse link = linkService.create(tenantId, userId, request);
            return Result.success(link);
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    /**
     * 删除工作项关系
     */
    @DeleteMapping("/{linkId}")
    public Result<Void> deleteLink(@PathVariable Long linkId) {
        try {
            linkService.delete(linkId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
