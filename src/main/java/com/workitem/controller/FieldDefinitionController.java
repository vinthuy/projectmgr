package com.workitem.controller;

import com.workitem.dto.*;
import com.workitem.service.FieldDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/field-definitions")
@RequiredArgsConstructor
public class FieldDefinitionController {

    private final FieldDefinitionService fieldDefinitionService;

    @PostMapping
    public Result<FieldDefinitionResponse> create(
            @RequestParam(required = false, defaultValue = "1") Long tenantId,
            @Valid @RequestBody FieldDefinitionCreateRequest request) {
        try {
            return Result.success(fieldDefinitionService.create(tenantId, request));
        } catch (RuntimeException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<FieldDefinitionResponse> update(@PathVariable Long id,
                                                   @RequestBody FieldDefinitionUpdateRequest request) {
        return Result.success(fieldDefinitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        fieldDefinitionService.delete(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<FieldDefinitionResponse> getById(@PathVariable Long id) {
        return Result.success(fieldDefinitionService.getById(id));
    }

    @GetMapping
    public Result<List<FieldDefinitionResponse>> list(
            @RequestParam(required = false, defaultValue = "1") Long tenantId) {
        return Result.success(fieldDefinitionService.listByTenant(tenantId));
    }
}
