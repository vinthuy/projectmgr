package com.workitem.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.workitem.dto.TenantCreateRequest;
import com.workitem.dto.TenantResponse;
import com.workitem.dto.TenantUpdateRequest;
import com.workitem.entity.Tenant;
import com.workitem.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService extends ServiceImpl<TenantMapper, Tenant> {

    /**
     * 获取所有租户
     */
    public List<TenantResponse> listAll() {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tenant::getCreatedAt);

        List<Tenant> tenants = this.list(wrapper);

        return tenants.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取租户
     */
    public TenantResponse getById(Long id) {
        Tenant tenant = this.baseMapper.selectById(id);
        if (tenant == null) {
            return null;
        }
        return convertToResponse(tenant);
    }

    /**
     * 根据租户Key获取租户
     */
    public TenantResponse getByKey(String tenantKey) {
        LambdaQueryWrapper<Tenant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tenant::getTenantKey, tenantKey);

        Tenant tenant = this.getOne(wrapper);
        return tenant != null ? convertToResponse(tenant) : null;
    }

    /**
     * 创建租户
     */
    @Transactional
    public TenantResponse create(TenantCreateRequest request) {
        // 检查租户Key是否已存在
        TenantResponse existing = getByKey(request.getTenantKey());
        if (existing != null) {
            throw new RuntimeException("租户标识已存在: " + request.getTenantKey());
        }

        Tenant tenant = new Tenant();
        BeanUtils.copyProperties(request, tenant);
        tenant.setStatus("ACTIVE");
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setUpdatedAt(LocalDateTime.now());

        this.save(tenant);
        return convertToResponse(tenant);
    }

    /**
     * 更新租户
     */
    @Transactional
    public TenantResponse update(Long id, TenantUpdateRequest request) {
        Tenant tenant = this.baseMapper.selectById(id);
        if (tenant == null) {
            throw new RuntimeException("租户不存在: " + id);
        }

        // 只更新非空字段
        if (request.getTenantName() != null) {
            tenant.setTenantName(request.getTenantName());
        }
        if (request.getDescription() != null) {
            tenant.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        if (request.getLicenseType() != null) {
            tenant.setLicenseType(request.getLicenseType());
        }
        if (request.getMaxUsers() != null) {
            tenant.setMaxUsers(request.getMaxUsers());
        }
        if (request.getMaxProjects() != null) {
            tenant.setMaxProjects(request.getMaxProjects());
        }

        tenant.setUpdatedAt(LocalDateTime.now());
        this.updateById(tenant);

        return convertToResponse(tenant);
    }

    /**
     * 删除租户（逻辑删除）
     */
    @Transactional
    public void delete(Long id) {
        Tenant tenant = this.baseMapper.selectById(id);
        if (tenant == null) {
            throw new RuntimeException("租户不存在: " + id);
        }

        this.removeById(id);
    }

    private TenantResponse convertToResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        BeanUtils.copyProperties(tenant, response);
        return response;
    }
}
